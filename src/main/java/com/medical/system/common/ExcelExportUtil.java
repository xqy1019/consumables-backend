package com.medical.system.common;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelWriter;
import com.alibaba.excel.write.metadata.WriteSheet;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import java.net.URLEncoder;
import java.util.List;

@Slf4j
public class ExcelExportUtil {

    public static <T> void export(HttpServletResponse response, String filename,
                                   Class<T> clazz, List<T> data) {
        try {
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setCharacterEncoding("utf-8");
            String encodedFilename = URLEncoder.encode(filename, "UTF-8").replaceAll("\\+", "%20");
            response.setHeader("Content-disposition", "attachment;filename*=utf-8''" + encodedFilename + ".xlsx");
            EasyExcel.write(response.getOutputStream(), clazz).sheet("数据").doWrite(data);
        } catch (Exception e) {
            log.error("Excel导出失败: {}", e.getMessage(), e);
            response.reset();
            response.setContentType("application/json;charset=utf-8");
            try {
                response.getWriter().write("{\"code\":500,\"message\":\"导出失败: " + e.getMessage() + "\"}");
            } catch (Exception ex) {
                log.error("写入错误响应失败", ex);
            }
        }
    }

    /**
     * 导出多 Sheet 的 Excel 文件。
     * @param sheetConfigs 每个 sheet 的配置：[sheetName, dataClass, dataList]
     */
    public static void exportMultiSheet(HttpServletResponse response, String filename,
                                         List<SheetConfig<?>> sheetConfigs) {
        try {
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setCharacterEncoding("utf-8");
            String encodedFilename = URLEncoder.encode(filename, "UTF-8").replaceAll("\\+", "%20");
            response.setHeader("Content-disposition", "attachment;filename*=utf-8''" + encodedFilename + ".xlsx");

            try (ExcelWriter excelWriter = EasyExcel.write(response.getOutputStream()).build()) {
                for (int i = 0; i < sheetConfigs.size(); i++) {
                    SheetConfig<?> config = sheetConfigs.get(i);
                    WriteSheet writeSheet = EasyExcel.writerSheet(i, config.sheetName())
                            .head(config.dataClass()).build();
                    excelWriter.write(config.dataList(), writeSheet);
                }
            }
        } catch (Exception e) {
            log.error("Excel多Sheet导出失败: {}", e.getMessage(), e);
            response.reset();
            response.setContentType("application/json;charset=utf-8");
            try {
                response.getWriter().write("{\"code\":500,\"message\":\"导出失败: " + e.getMessage() + "\"}");
            } catch (Exception ex) {
                log.error("写入错误响应失败", ex);
            }
        }
    }

    public record SheetConfig<T>(String sheetName, Class<T> dataClass, List<T> dataList) {}
}
