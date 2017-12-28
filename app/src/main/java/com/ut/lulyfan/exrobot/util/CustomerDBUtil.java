package com.ut.lulyfan.exrobot.util;

import com.ut.lulyfan.exrobot.model.Customer;

import org.greenrobot.greendao.Property;
import org.greenrobot.greendao.query.CloseableListIterator;
import org.greenrobot.greendao.query.Query;
import org.greenrobot.greendao.query.WhereCondition;

import java.util.List;

import greenDao.CustomerDao;
import greenDao.DaoSession;

/**
 * Created by Administrator on 2017/10/31/031.
 */

public class CustomerDBUtil {

    private DaoSession daoSession;
    private CustomerDao customerDao;
    private static CustomerDBUtil customerDBUtil;

    public synchronized static CustomerDBUtil getInstance(DaoSession daoSession) {
        if (customerDBUtil == null) {
            customerDBUtil = new CustomerDBUtil(daoSession);
        }
        return customerDBUtil;
    }

    public synchronized static CustomerDBUtil getInstance() {
        return customerDBUtil;
    }

    private CustomerDBUtil(DaoSession daoSession) {
        if (daoSession == null) {
            throw new NullPointerException("daoSession = null!");
        }

        this.daoSession = daoSession;
        customerDao = this.daoSession.getCustomerDao();
    }

    public void write(Customer customer) {
        customerDao.insert(customer);
    }

    /**
     * 查询属性=value的对象
     * use like this：查询ErrorCode=5的对象
     * List<RosError> rosErrors = logInDB.readRosError(RosErrorDao.Properties.ErrorCode, 5);
     */
    public List<Customer> queryCustomer(Property property, Object value) {
        return customerDao.queryBuilder()
                .where(property.eq(value))
                .list();
    }

    /**
     * 根据一个或多个条件查询对象
     * use like this：查询ErrorCode>5的对象：
     *  List<RosError> rosErrors2 = logInDB.readRosError(RosErrorDao.Properties.ErrorCode.gt(5);
     *  查询 5<ErrorCode<8 的对象：
     *    List<RosError> rosErrors2 = logInDB.readRosError(RosErrorDao.Properties.ErrorCode.gt(5),RosErrorDao.Properties.ErrorCode.lt(8));
     */
    public List queryCustomer(WhereCondition condition, WhereCondition... conditions) {
        return customerDao.queryBuilder()
                .where(condition, conditions)
                .list();
    }

    /**
     * 根据where子句查询对象
     * @param where
     * use like this：
     * List<RosError> rosErrors3 = logInDB.readRosError("ERROR_CODE = 2");
     * @return
     */
    public List queryCustomer(String where) {
        Query query = customerDao.queryBuilder().where(
                new WhereCondition.StringCondition(where)
        ).build();
        return query.list();
    }


    /**
     *use like this:
     LogInDB logInDB = new LogInDB();
     CloseableListIterator<RosError> iterator = logInDB.readAllRosErrorByIterator();
     while (iterator.hasNext())
     RosError rosError = iterator.next();
     try {
     iterator.close();
     } catch (IOException e) {
     e.printStackTrace();
     }
     * @return
     */
    public CloseableListIterator<Customer> queryAllCustomerByIterator() {
        return customerDao.queryBuilder()
                .where(CustomerDao.Properties.Id.ge(0))
                .listIterator();
    }

    public List queryAllRosError() {
        return customerDao.loadAll();
    }

    public void deleteAll() {
        customerDao.deleteAll();
    }

    public void delete(Customer customer) {
        customerDao.delete(customer);
    }

    public void update(Customer customer) {
        customerDao.update(customer);
    }
}
