package org.example;

import com.google.common.util.concurrent.RateLimiter;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 压测 java 脚本
 * 其中 t-o-d-o 部分进行添加或者修改一下逻辑即可！
 */
public class LoadTesting {
    // ⭐️设置启动线程数量
    // todo: 依据压测情况你可以实际调整
    private static final int threadCount = 1;

    // ⭐️压测持续时间 (秒 s)
    // todo: 依据压测情况你可以实际调整
    private static final int duration = 10;

    // ⭐️限制 QPS 绝对不超过多少（限制最大 QPS 阈值）
    // todo: 依据压测情况你可以实际调整
    private static final int maxQps = 5;
    private static final RateLimiter rateLimiter = RateLimiter.create((double) maxQps);


    // ExecutorService 线程池
    private static ExecutorService pool;


    // 统计总请求数据
    private static final AtomicLong totalReqCount = new AtomicLong(0);
    // 统计失败请求数
    private static final AtomicLong failedReqCount = new AtomicLong(0);
    // 总响应时长 ms
    private static final AtomicLong totalRespTime = new AtomicLong(0);
    // 记录每个请求响应时间，用于计算 tp99 等
    private static final CopyOnWriteArrayList<Long> tpReqList = new CopyOnWriteArrayList<>();


    /**
     * 压测某个接口
     *
     * @param args args
     */
    public static void main(String[] args) throws Exception {
        /* 开始 */
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String formatted = now.format(formatter);
        System.out.printf("====== 压测代码开始执行，当前时间 %s\n", formatted);

        /* 初始化接口 */
        initApi();
        /* 初始化线程池 */
        initThreadPool();

        /* 循环 threadCount 次，相当于启动 threadCount 个线程来压 */
        for (int i = 0; i < threadCount; i++) {
            // async 压测该接口
            pool.submit(LoadTesting::load4OneThread);
        }

        /* duration 时间到了就要停止压测 */
        Thread.sleep(duration * 1000);
        System.out.printf("====== %d 秒 时间到了，正在终止...\n", duration);
        pool.shutdown();
        Thread.sleep(3000);

        /* 结束打印结果 */
        now = LocalDateTime.now();
        formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        formatted = now.format(formatter);
        System.out.printf("====== 压测代码结束执行，当前时间 %s\n", formatted);
        genResult(); // 打印统计结果
    }

    // 初始化 scf
    private static void initApi() {
        // todo 初始化要被压测的接口，比如要压 rpc 接口，先要建立 rpc 连接
    }

    // 初始化线程池 ThreadPoolExecutor
    private static void initThreadPool() {
        pool = new ThreadPoolExecutor(
                threadCount, // 核心线程数
                threadCount, // 最大线程数
                10L, TimeUnit.SECONDS, // 空闲线程存活时间
                new LinkedBlockingQueue<>(100), // 任务队列 todo: 依据压测情况你可以实际调整（一般不调整即可）
                new ThreadPoolExecutor.CallerRunsPolicy() // 拒绝策略
        );
    }

    // 调用 scf 接口请求沙箱，查询代招标签

    /**
     * 一个线程调用接口请求来进行压测
     */
    private static void load4OneThread() {
        // 构造满足条件的随机的接口请求参数，作为接口压力请求的入参
        String reqParam = getRandomReqParam();
        // 接口调用返回结果
        String reqResult;

        // 该次请求开始时间
        long reqBeginTime;
        // 该次请求结束时间
        long reqEndTime;

        // 计算出当前时间 + 预计要压力测试持续的时间后，结束的时间为 endTime
        long endTime = System.currentTimeMillis() + (duration * 1000);
        // 持续请求该接口，直到当前时间达到了预先填写的持续时间
        while (System.currentTimeMillis() < endTime) {
            // acquire() 会阻塞直到可以获取到令牌
            rateLimiter.acquire();

            // 调用 api
            reqBeginTime = System.currentTimeMillis(); // 记录该次请求开始时间
            try {
                reqResult = invokeApi(reqParam);
            } catch (Exception ignored) {
                failedReqCount.incrementAndGet(); // 失败请求次数++
            } finally {
                reqEndTime = System.currentTimeMillis(); // 记录该次请求结束时间
                totalReqCount.incrementAndGet(); // 请求总次数++
            }

            totalRespTime.addAndGet(reqEndTime - reqBeginTime); // 加上响应时间
            tpReqList.add(reqEndTime - reqBeginTime); // list add
            // todo: 这里还可以对 reqResult 进行判断，用来统计错误率
        }
    }

    /**
     * 生成满足条件的随机的请求入参
     *
     * @return 随机的请求入参
     */
    private static String getRandomReqParam() {
        // todo: 生成满足条件的随机的请求入参
        return null;
    }

    /**
     * 直接进行接口调用
     *
     * @param reqParam 接口请求入参
     * @return 接口返回结果
     */
    private static String invokeApi(String reqParam) {
        // todo: 进行接口调用
        return null;
    }

    /**
     * 生成压测结果
     */
    private static void genResult() {
        long total = totalReqCount.get(); // 总请求数
        long failed = failedReqCount.get(); // 失败请求数
        long success = total - failed; // 成功请求数
        double avg = total == 0 ? 0 : totalRespTime.get() / (double) total; // 平均响应耗时 ms

        System.out.print("\n\n============\n");
        System.out.printf("总请求数: %d\n", total);
        System.out.printf("成功请求数: %d，成功率占比: %.2f%%\n", success, success * 100.0 / total);
        System.out.print("============\n");
        System.out.printf("总时长: %d 秒\n", duration);
        System.out.printf("平均响应耗时: %.2f ms\n", avg);
        System.out.print("============\n");
        List<Long> sortedList = new ArrayList<>(tpReqList);
        Collections.sort(sortedList); // 每个请求响应耗时从小到大排序
        long tp999 = sortedList.get(Math.max(sortedList.size() * 999 / 1000 - 1, 0));
        long tp99 = sortedList.get(Math.max(sortedList.size() * 99 / 100 - 1, 0));
        long tp90 = sortedList.get(Math.max(sortedList.size() * 90 / 100 - 1, 0));
        long tp80 = sortedList.get(Math.max(sortedList.size() * 80 / 100 - 1, 0));
        long tp70 = sortedList.get(Math.max(sortedList.size() * 70 / 100 - 1, 0));
        long tp60 = sortedList.get(Math.max(sortedList.size() * 60 / 100 - 1, 0));
        long tp50 = sortedList.get(Math.max(sortedList.size() * 50 / 100 - 1, 0));
        System.out.printf("tp999: %d ms，tp99: %d ms, tp90: %d ms, tp80: %d ms, tp70: %d ms, tp60: %d ms, tp50: %d ms\n", tp999, tp99, tp90, tp80, tp70, tp60, tp50);
        System.out.print("============\n");
    }
}
