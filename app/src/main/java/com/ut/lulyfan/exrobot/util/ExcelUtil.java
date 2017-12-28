package com.ut.lulyfan.exrobot.util;

import com.ut.lulyfan.exrobot.model.Customer;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ExcelUtil {

    public static List<Customer> parse(String path) throws InvalidFormatException, IOException {

        File file = new File(path);
        Workbook wb = WorkbookFactory.create(file);
        Sheet sheet1 = wb.getSheetAt(0);
        DataFormatter formatter = new DataFormatter();

        List<Customer> customers = new ArrayList<>();

        int phoneNumIndex = 0;
        int nameIndex = 0;
        int regionIndex = 0;
        int xIndex = 0;
        int yIndex = 0;
        int wIndex = 0;
        int zIndex = 0;

        int lastColNum = 0;

        for (Row row : sheet1) {
            if (row.getRowNum() == sheet1.getFirstRowNum()) {

                lastColNum = row.getLastCellNum();
                for (Cell cell : row) {
                    int colNum = cell.getColumnIndex();
                    String str = cell.getStringCellValue();
                    System.out.print(str + "\t");
                    if (str.contains("手机")) {
                        phoneNumIndex = colNum;
                    } else if (str.contains("姓名") || str.contains("名字")) {
                        nameIndex = colNum;
                    } else if ("部门".equals(str)) {

                    } else if ("区域".equals(str) || "座位".equals(str)) {
                        regionIndex = colNum;
                    } else if ("x".equalsIgnoreCase(str)) {
                        xIndex = colNum;
                    } else if ("y".equalsIgnoreCase(str)) {
                        yIndex = colNum;
                    } else if ("w".equalsIgnoreCase(str)) {
                        wIndex = colNum;
                    } else if ("z".equalsIgnoreCase(str)) {
                        zIndex = colNum;
                    }
                }

            } else {

                if (row.getCell(xIndex) != null) {
                    if ("".equals(formatter.formatCellValue(row.getCell(xIndex)))) {
                        continue;
                    }
                }

                Customer customer = new Customer();
                for (int i = 0; i < lastColNum; i++) {
                    Cell cell = row.getCell(i);
                    String str = cell == null ? "" : formatter.formatCellValue(cell);
                    System.out.print(str + "\t");

                    if (i == phoneNumIndex) {
                        customer.setPhoneNum(str);
                    } else if (i == nameIndex) {
                        customer.setName(str);
                    } else if (i == regionIndex) {
                        customer.setArea(str);
                    } else if (i == xIndex) {
                        customer.setX(Double.valueOf(str));
                    } else if (i == yIndex) {
                        customer.setY(Double.valueOf(str));
                    } else if (i == wIndex) {
                        customer.setW(Double.valueOf(str));
                    } else if (i == zIndex) {
                        customer.setZ(Double.valueOf(str));
                    }
                }
                customers.add(customer);
            }
            System.out.println();
        }
        return customers;
    }
}
