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
                    } else if (str.equals("部门")) {

                    } else if (str.equals("区域") || str.equals("座位")) {
                        regionIndex = colNum;
                    } else if (str.equalsIgnoreCase("x")) {
                        xIndex = colNum;
                    } else if (str.equalsIgnoreCase("y")) {
                        yIndex = colNum;
                    } else if (str.equalsIgnoreCase("w")) {
                        wIndex = colNum;
                    } else if (str.equalsIgnoreCase("z")) {
                        zIndex = colNum;
                    }
                }

            } else {

                if (row.getCell(xIndex) != null) {
                    if (formatter.formatCellValue(row.getCell(xIndex)).equals(""))
                        continue;
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
