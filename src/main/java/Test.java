import cn.hutool.core.date.DateUtil;
import cn.hutool.core.date.TimeInterval;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.thread.ExecutorBuilder;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.ReUtil;

import java.io.File;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Test {

    private static final ExecutorService executor = ExecutorBuilder.create()
            // 设置初始池大小
            .setCorePoolSize(8)
            // 设置最大池大小
            .setMaxPoolSize(8)
            .setWorkQueue(new LinkedBlockingQueue<>(Integer.MAX_VALUE))
            .build();

    public static void main(String[] args) {
        TimeInterval timer = DateUtil.timer();
        clearDir("/Volumes/wushuo/Media/番剧");
        while (((ThreadPoolExecutor) executor).getActiveCount() > 0) {
            ThreadUtil.sleep(500);
        }
        executor.shutdown();
        System.out.printf("耗时%s分钟%n", timer.intervalMinute());
    }

    public static void clearDir(String path) {
        if (!FileUtil.exist(path)) {
            return;
        }
        if (FileUtil.isFile(path)) {
            return;
        }
        File[] files = FileUtil.ls(path);
        if (Objects.isNull(files)) {
            return;
        }
        // 过滤出视频文件并获取主文件名
        List<String> mediaFiles =
                Stream.of(files)
                        .filter(FileUtil::isFile)
                        .filter(f -> List.of("mkv", "mp4").contains(FileUtil.extName(f)))
                        .map(FileUtil::mainName)
                        .collect(Collectors.toList());

        // 过滤出匹配不到对应视频文件的 .nfo、-thumb.jpg
        Stream.of(files)
                .filter(FileUtil::isFile)
                .filter(file -> {
                    String name = file.getName();
                    return ReUtil.contains("[Ss](\\d+)[Ee](\\d+)", name)
                            && (name.endsWith(".nfo") || name.endsWith("-thumb.jpg"));
                })
                .filter(file -> {
                    String mainName = FileUtil.mainName(file);
                    if (mainName.endsWith("-thumb")) {
                        mainName = mainName.replace("-thumb", "");
                    }
                    return !mediaFiles.contains(mainName);
                })
                .forEach(file -> {
                    System.out.println(file);
                    FileUtil.del(file);
                });

        Stream.of(files)
                .filter(FileUtil::isDirectory)
                .map(File::toString)
                .map(FileUtil::getAbsolutePath)
                .forEach(newPath -> executor.submit(() -> Test.clearDir(newPath)));
    }

}
