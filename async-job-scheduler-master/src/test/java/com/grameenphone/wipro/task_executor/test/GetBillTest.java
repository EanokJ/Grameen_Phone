package com.grameenphone.wipro.task_executor.test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.grameenphone.wipro.task_executor.Main;
import com.grameenphone.wipro.task_executor.config.CbpDbConnectionPool;
import com.grameenphone.wipro.task_executor.enums.BillStatus;
import com.grameenphone.wipro.task_executor.model.api.DueBillResponse;
import com.grameenphone.wipro.task_executor.model.orm.cbp.Bill;
import com.grameenphone.wipro.task_executor.model.orm.cbp.BillRevertibleCache;
import com.grameenphone.wipro.task_executor.repository.CrudDao;
import com.grameenphone.wipro.task_executor.processors.BillCollector;
import com.grameenphone.wipro.task_executor.util.JsonUtil;
import com.grameenphone.wipro.task_executor.util.MockHttpClient;
import com.grameenphone.wipro.task_executor.util.MockHttpClient.MockCondition;
import com.grameenphone.wipro.task_executor.util.MockHttpClient.NotifyCondition;
import com.grameenphone.wipro.task_executor.util.PropertyUtil;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;

/**
 * @Pre_Requisites A user with name <pre>" + userName + "</pre>
 */
public class GetBillTest extends TestBaseContext {
    static String nodeId;
    static String notificationUrl;
    static String dueBillUrl = PropertyUtil.getProperty("api_due_bill_url");

    String userClient1 = "Client 1 All Division";
    String userClient2 = "Client 2 All Division";
    String category1 = "ELEC POST";
    String company1 = "DPDC";
    String company2 = "BREB";
    String company5 = "WZPD";
    String company6 = "NWZPD";
    String company7 = "DSCO";
    String category2 = "ELEC PRE";
    String company3 = "PDBPP";
    String company4 = "REBP";
    String company8 = "DPDCP";
    String company9 = "DSCOP";
    String company10 = "WZPDP";
    String company11 = "RVRT";
    String client1 = "Test Client 1";
    String division1 = "Test Client 1 Division 1";
    String division2 = "Test Client 1 Division 2";
    String client2 = "Test Client 2";
    String division3 = "Test Client 2 Division 1";
    String division4 = "Test Client 2 Division 2";
    String account1 = "123456";
    String account2 = "789012";
    String[] bills = new String[] {"111111111111", "222222222222", "333333333333", "444444444444"};

    static NotifyCondition dueBillApiCallNotifier;
    static MockCondition dueBillMock;

    static {
        nodeId = PropertyUtil.getProperty("task_executor_node_id");
        notificationUrl = PropertyUtil.getProperty("notification_due_bill");
        MockHttpClient.forUrl(notificationUrl).response(200, "OK");
        dueBillMock = MockHttpClient.forUrl(dueBillUrl);
        dueBillMock.response(200, "OK");
        dueBillApiCallNotifier = MockHttpClient.notifyFor(dueBillUrl);
    }
    
    private String getBillDataInsertionQuery(String division, String user, String company, String account) {
        return "INSERT INTO `bill_data` (`client_division_id`, `added_by_id`, `updated_by_id`, `company_id`, `account_no`, `msisdn`) VALUES ((select id from client_division where name = '" + division + "'), (select id from user where name = '" + user + "'), (select id from user where name = '" + user + "'), (select id from company where code = '" + company + "'), '" + account + "', 1711082546)";
    }

    @BeforeClass
    public void insertAllBillData() throws SQLException {
        System.out.println("Inserting sample Bill Data");
        try(Connection connection = CbpDbConnectionPool.getConnection(); Statement statement = connection.createStatement()) {
            statement.executeUpdate(getBillDataInsertionQuery(division1, userClient1, company1, account1));
            statement.executeUpdate(getBillDataInsertionQuery(division1, userClient1, company1, account2));
            statement.executeUpdate(getBillDataInsertionQuery(division2, userClient1, company5, account1));
            statement.executeUpdate(getBillDataInsertionQuery(division2, userClient1, company5, account2));
            statement.executeUpdate(getBillDataInsertionQuery(division3, userClient2, company6, account1));
            statement.executeUpdate(getBillDataInsertionQuery(division3, userClient2, company6, account2));
            statement.executeUpdate(getBillDataInsertionQuery(division4, userClient2, company7, account1));
            statement.executeUpdate(getBillDataInsertionQuery(division4, userClient2, company7, account2));
            statement.executeUpdate(getBillDataInsertionQuery(division1, userClient1, company3, account1));
            statement.executeUpdate(getBillDataInsertionQuery(division1, userClient1, company3, account2));
            statement.executeUpdate(getBillDataInsertionQuery(division1, userClient1, company2, account1));
            statement.executeUpdate(getBillDataInsertionQuery(division1, userClient1, company2, account2));
            statement.executeUpdate(getBillDataInsertionQuery(division2, userClient1, company8, account1));
            statement.executeUpdate(getBillDataInsertionQuery(division2, userClient1, company8, account2));
            statement.executeUpdate(getBillDataInsertionQuery(division3, userClient2, company9, account1));
            statement.executeUpdate(getBillDataInsertionQuery(division3, userClient2, company9, account2));
            statement.executeUpdate(getBillDataInsertionQuery(division4, userClient2, company10, account1));
            statement.executeUpdate(getBillDataInsertionQuery(division4, userClient2, company10, account2));
            statement.executeUpdate(getBillDataInsertionQuery(division1, userClient1, company4, account1));
            statement.executeUpdate(getBillDataInsertionQuery(division1, userClient1, company4, account2));
        }
    }

    @BeforeMethod
    public void allCaseInit() throws SQLException {
        Main.openSession();
    }

    @BeforeMethod(onlyForGroups = "case1")
    public void case1Init() throws SQLException {
        System.out.println("Preparing All Case 1 Data");
        try(Connection connection = CbpDbConnectionPool.getConnection(); Statement statement = connection.createStatement()) {
            String query = "INSERT INTO `bill_detail_task` (`added_by_id`, `status`, `company_id`, `account_no`, `client_id`, `node_id`, `category_id`) VALUES ((select id from user where name = '" + userClient1 + "'), 'Pending', (select id from company where code = '" + company1 + "'), '" + account1 + "', (select id from client where name = '" + client1 + "'), '" + nodeId + "', (select id from category where code = '" + category1 + "'))";
            statement.executeUpdate(query);
            query = "INSERT INTO `bill_detail_task_client_divisions` (`bill_detail_task_id`, `client_divisions_id`) VALUES (last_insert_id(), (select id from client_division where name = '" + division1 + "'))";
            statement.executeUpdate(query);
        }
        dueBillApiCallNotifier.resetCount();
    }

    @Test(groups = "case1")
    /**
     * Account No Exists, Company Exists, Client Division Exists
     */
    public void billCollectionCase1() {
        System.out.println("Started Test: Bill Collection Case 1");
        new BillCollector().run();
        Assert.assertEquals(dueBillApiCallNotifier.getCallCount(), 1, "Bill Data Count Not Matched");
    }

    @BeforeMethod(onlyForGroups = "case2")
    public void case2Init() throws SQLException {
        System.out.println("Preparing All Case 2 Data");
        try(Connection connection = CbpDbConnectionPool.getConnection(); Statement statement = connection.createStatement()) {
            String query = "INSERT INTO `bill_detail_task` (`added_by_id`, `status`, `company_id`, `account_no`, `client_id`, `node_id`, `category_id`) VALUES ((select id from user where name = '" + userClient1 + "'), 'Pending', (select id from company where code = '" + company1 + "'), '" + account1 + "', (select id from client where name = '" + client1 + "'), '" + nodeId + "', (select id from category where code = '" + category1 + "'))";
            statement.executeUpdate(query);
        }
        dueBillApiCallNotifier.resetCount();
    }

    @Test(groups = "case2")
    /**
     * Account No Exists, Company Exists (Has Bill), No Client Division
     */
    public void billCollectionCase2() {
        System.out.println("Started Test: Bill Collection Case 2");
        new BillCollector().run();
        Assert.assertEquals(dueBillApiCallNotifier.getCallCount(), 1, "Bill Data Count Not Matched");
    }

    @BeforeMethod(onlyForGroups = "case3")
    public void case3Init() throws SQLException {
        System.out.println("Preparing All Case 3 Data");
        try(Connection connection = CbpDbConnectionPool.getConnection(); Statement statement = connection.createStatement()) {
            String query = "INSERT INTO `bill_detail_task` (`added_by_id`, `status`, `company_id`, `client_id`, `node_id`, `category_id`) VALUES ((select id from user where name = '" + userClient1 + "'), 'Pending', (select id from company where code = '" + company1 + "'), (select id from client where name = '" + client1 + "'), '" + nodeId + "', (select id from category where code = '" + category1 + "'))";
            statement.executeUpdate(query);
            query = "INSERT INTO `bill_detail_task_client_divisions` (`bill_detail_task_id`, `client_divisions_id`) VALUES (last_insert_id(), (select id from client_division where name = '" + division1 + "'))";
            statement.executeUpdate(query);
        }
        dueBillApiCallNotifier.resetCount();
    }

    @Test(groups = "case3")
    /**
     * No Account No, Company Exists, Client Division Exists
     */
    public void billCollectionCase3() {
        System.out.println("Started Test: Bill Collection Case 3");
        new BillCollector().run();
        Assert.assertEquals(dueBillApiCallNotifier.getCallCount(), 2, "Bill Data Count Not Matched");
    }

    @BeforeMethod(onlyForGroups = "case4")
    public void case4Init() throws SQLException {
        System.out.println("Preparing All Case 4 Data");
        try(Connection connection = CbpDbConnectionPool.getConnection(); Statement statement = connection.createStatement()) {
            String query = "INSERT INTO `bill_detail_task` (`added_by_id`, `status`, `company_id`, `client_id`, `node_id`, `category_id`) VALUES ((select id from user where name = '" + userClient1 + "'), 'Pending', (select id from company where code = '" + company1 + "'), (select id from client where name = '" + client1 + "'), '" + nodeId + "', (select id from category where code = '" + category1 + "'))";
            statement.executeUpdate(query);
        }
        dueBillApiCallNotifier.resetCount();
    }

    @Test(groups = "case4")
    /**
     * No Account No, Company Exists (Has Bill), No Client Division
     */
    public void billCollectionCase4() {
        System.out.println("Started Test: Bill Collection Case 4");
        new BillCollector().run();
        Assert.assertEquals(dueBillApiCallNotifier.getCallCount(), 2, "Bill Data Count Not Matched");
    }

    @BeforeMethod(onlyForGroups = "case5")
    public void case5Init() throws SQLException {
        System.out.println("Preparing All Case 5 Data");
        try(Connection connection = CbpDbConnectionPool.getConnection(); Statement statement = connection.createStatement()) {
            String query = "INSERT INTO `bill_detail_task` (`added_by_id`, `status`, `account_no`, `client_id`, `node_id`, `category_id`) VALUES ((select id from user where name = '" + userClient1 + "'), 'Pending', '" + account1 + "', (select id from client where name = '" + client1 + "'), '" + nodeId + "', (select id from category where code = '" + category1 + "'))";
            statement.executeUpdate(query);
            query = "INSERT INTO `bill_detail_task_client_divisions` (`bill_detail_task_id`, `client_divisions_id`) VALUES (last_insert_id(), (select id from client_division where name = '" + division1 + "'))";
            statement.executeUpdate(query);
        }
        dueBillApiCallNotifier.resetCount();
    }

    @Test(groups = "case5")
    /**
     * Account No Exists, Only Category (Has Bill), Client Division Exists
     */
    public void billCollectionCase5() {
        System.out.println("Started Test: Bill Collection Case 5");
        new BillCollector().run();
        Assert.assertEquals(dueBillApiCallNotifier.getCallCount(), 2, "Bill Data Count Not Matched");
    }

    @BeforeMethod(onlyForGroups = "case6")
    public void case6Init() throws SQLException {
        System.out.println("Preparing All Case 6 Data");
        try(Connection connection = CbpDbConnectionPool.getConnection(); Statement statement = connection.createStatement()) {
            String query = "INSERT INTO `bill_detail_task` (`added_by_id`, `status`, `account_no`, `client_id`, `node_id`, `category_id`) VALUES ((select id from user where name = '" + userClient1 + "'), 'Pending', '" + account1 + "', (select id from client where name = '" + client1 + "'), '" + nodeId + "', (select id from category where code = '" + category1 + "'))";
            statement.executeUpdate(query);
        }
        dueBillApiCallNotifier.resetCount();
    }

    @Test(groups = "case6")
    /**
     * Account No Exists, Only Category (Has Bill), No Client Division
     */
    public void billCollectionCase6() {
        System.out.println("Started Test: Bill Collection Case 6");
        new BillCollector().run();
        Assert.assertEquals(dueBillApiCallNotifier.getCallCount(), 3, "Bill Data Count Not Matched");
    }

    @BeforeMethod(onlyForGroups = "case7")
    public void case7Init() throws SQLException {
        System.out.println("Preparing All Case 7 Data");
        try(Connection connection = CbpDbConnectionPool.getConnection(); Statement statement = connection.createStatement()) {
            String query = "INSERT INTO `bill_detail_task` (`added_by_id`, `status`, `client_id`, `node_id`, `category_id`) VALUES ((select id from user where name = '" + userClient1 + "'), 'Pending', (select id from client where name = '" + client1 + "'), '" + nodeId + "', (select id from category where code = '" + category1 + "'))";
            statement.executeUpdate(query);
            query = "INSERT INTO `bill_detail_task_client_divisions` (`bill_detail_task_id`, `client_divisions_id`) VALUES (last_insert_id(), (select id from client_division where name = '" + division1 + "'))";
            statement.executeUpdate(query);
        }
        dueBillApiCallNotifier.resetCount();
    }

    @Test(groups = "case7")
    /**
     * No Account No, Only Category (Has Bill), Client Division Exists
     */
    public void billCollectionCase7() {
        System.out.println("Started Test: Bill Collection Case 7");
        new BillCollector().run();
        Assert.assertEquals(dueBillApiCallNotifier.getCallCount(), 4, "Bill Data Count Not Matched");
    }

    @BeforeMethod(onlyForGroups = "case8")
    public void case8Init() throws SQLException {
        System.out.println("Preparing All Case 8 Data");
        try(Connection connection = CbpDbConnectionPool.getConnection(); Statement statement = connection.createStatement()) {
            String query = "INSERT INTO `bill_detail_task` (`added_by_id`, `status`, `client_id`, `node_id`, `category_id`) VALUES ((select id from user where name = '" + userClient1 + "'), 'Pending', (select id from client where name = '" + client1 + "'), '" + nodeId + "', (select id from category where code = '" + category1 + "'))";
            statement.executeUpdate(query);
        }
        dueBillApiCallNotifier.resetCount();
    }

    @Test(groups = "case8")
    /**
     * No Account No, Only Category (Has Bill), No Client Division
     */
    public void billCollectionCase8() {
        System.out.println("Started Test: Bill Collection Case 8");
        new BillCollector().run();
        Assert.assertEquals(dueBillApiCallNotifier.getCallCount(), 6, "Bill Data Count Not Matched");
    }

    @BeforeMethod(onlyForGroups = "case9")
    public void case9Init() throws SQLException {
        System.out.println("Preparing All Case 9 Data");
        try(Connection connection = CbpDbConnectionPool.getConnection(); Statement statement = connection.createStatement()) {
            String query = "INSERT INTO `bill_detail_task` (`added_by_id`, `status`, `account_no`, `client_id`, `node_id`) VALUES ((select id from user where name = '" + userClient1 + "'), 'Pending', '" + account1 + "', (select id from client where name = '" + client1 + "'), '" + nodeId + "')";
            statement.executeUpdate(query);
            query = "INSERT INTO `bill_detail_task_client_divisions` (`bill_detail_task_id`, `client_divisions_id`) VALUES (last_insert_id(), (select id from client_division where name = '" + division1 + "'))";
            statement.executeUpdate(query);
        }
        dueBillApiCallNotifier.resetCount();
    }

    public long getBillCount() throws SQLException {
        return CrudDao.get(Bill.class).count();
    }

    public long getRevertiblesCount() throws SQLException {
        return CrudDao.get(BillRevertibleCache.class).count();
    }

    public long getBillCount(BillStatus status) throws SQLException {
        return CrudDao.get(Bill.class).query().eq("status", status).count();
    }

    @Test(groups = "case9")
    /**
     * Account No Exists, No Category, Client Division Exists
     */
    public void billCollectionCase9() throws SQLException {
        System.out.println("Started Test: Bill Collection Case 9");
        new BillCollector().run();
        Assert.assertEquals(dueBillApiCallNotifier.getCallCount() + getBillCount(), 4, "Bill Data Count Not Matched");
    }

    @BeforeMethod(onlyForGroups = "case10")
    public void case10Init() throws SQLException {
        System.out.println("Preparing All Case 10 Data");
        try(Connection connection = CbpDbConnectionPool.getConnection(); Statement statement = connection.createStatement()) {
            String query = "INSERT INTO `bill_detail_task` (`added_by_id`, `status`, `account_no`, `client_id`, `node_id`) VALUES ((select id from user where name = '" + userClient1 + "'), 'Pending', '" + account1 + "', (select id from client where name = '" + client1 + "'), '" + nodeId + "')";
            statement.executeUpdate(query);
        }
        dueBillApiCallNotifier.resetCount();
    }

    @Test(groups = "case10")
    /**
     * Account No Exists, No Category, No Client Division
     */
    public void billCollectionCase10() throws SQLException {
        System.out.println("Started Test: Bill Collection Case 10");
        new BillCollector().run();
        Assert.assertEquals(dueBillApiCallNotifier.getCallCount() + getBillCount(), 6, "Bill Data Count Not Matched");
    }

    @BeforeMethod(onlyForGroups = "case11")
    public void case11Init() throws SQLException {
        System.out.println("Preparing All Case 11 Data");
        try(Connection connection = CbpDbConnectionPool.getConnection(); Statement statement = connection.createStatement()) {
            String query = "INSERT INTO `bill_detail_task` (`added_by_id`, `status`, `client_id`, `node_id`) VALUES ((select id from user where name = '" + userClient1 + "'), 'Pending', (select id from client where name = '" + client1 + "'), '" + nodeId + "')";
            statement.executeUpdate(query);
            query = "INSERT INTO `bill_detail_task_client_divisions` (`bill_detail_task_id`, `client_divisions_id`) VALUES (last_insert_id(), (select id from client_division where name = '" + division1 + "'))";
            statement.executeUpdate(query);
        }
        dueBillApiCallNotifier.resetCount();
    }

    @Test(groups = "case11")
    /**
     * No Account No, Company Exists, Client Division Exists
     */
    public void billCollectionCase11() throws SQLException {
        System.out.println("Started Test: Bill Collection Case 11");
        new BillCollector().run();
        Assert.assertEquals(dueBillApiCallNotifier.getCallCount() + getBillCount(), 8, "Bill Data Count Not Matched");
    }

    @BeforeMethod(onlyForGroups = "case12")
    public void case12Init() throws SQLException {
        System.out.println("Preparing All Case 12 Data");
        try(Connection connection = CbpDbConnectionPool.getConnection(); Statement statement = connection.createStatement()) {
            String query = "INSERT INTO `bill_detail_task` (`added_by_id`, `status`, `client_id`, `node_id`) VALUES ((select id from user where name = '" + userClient1 + "'), 'Pending', (select id from client where name = '" + client1 + "'), '" + nodeId + "')";
            statement.executeUpdate(query);
        }
        dueBillApiCallNotifier.resetCount();
    }

    @Test(groups = "case12")
    /**
     * No Account No, Company Exists (Has Bill), No Client Division
     */
    public void billCollectionCase12() throws SQLException {
        System.out.println("Started Test: Bill Collection Case 12");
        new BillCollector().run();
        Assert.assertEquals(dueBillApiCallNotifier.getCallCount() + getBillCount(), 12, "Bill Data Count Not Matched");
    }

    @BeforeMethod(onlyForGroups = "caseMulti")
    public void caseMultiInit() throws SQLException {
        System.out.println("Preparing All Case Multi Task Data");
        try(Connection connection = CbpDbConnectionPool.getConnection(); Statement statement = connection.createStatement()) {
            String query = "INSERT INTO `bill_detail_task` (`added_by_id`, `status`, `client_id`, `node_id`) VALUES ((select id from user where name = '" + userClient1 + "'), 'Pending', (select id from client where name = '" + client1 + "'), '" + nodeId + "')";
            statement.executeUpdate(query);
            query = "INSERT INTO `bill_detail_task` (`added_by_id`, `status`, `company_id`, `account_no`, `client_id`, `node_id`, `category_id`) VALUES ((select id from user where name = '" + userClient1 + "'), 'Pending', (select id from company where code = '" + company1 + "'), '" + account1 + "', (select id from client where name = '" + client1 + "'), '" + nodeId + "', (select id from category where code = '" + category1 + "'))";
            statement.executeUpdate(query);
            query = "INSERT INTO `bill_detail_task_client_divisions` (`bill_detail_task_id`, `client_divisions_id`) VALUES (last_insert_id(), (select id from client_division where name = '" + division1 + "'))";
            statement.executeUpdate(query);
            query = "INSERT INTO `bill_detail_task` (`added_by_id`, `status`, `account_no`, `client_id`, `node_id`) VALUES ((select id from user where name = '" + userClient1 + "'), 'Pending', '" + account1 + "', (select id from client where name = '" + client1 + "'), '" + nodeId + "')";
            statement.executeUpdate(query);
        }
        dueBillApiCallNotifier.resetCount();
    }

    @Test(groups = "caseMulti")
    /**
     * Multiple Task
     */
    public void billCollectionCaseMulti() throws SQLException {
        System.out.println("Started Test: Bill Collection Case Multi Task");
        new BillCollector().run();
        Assert.assertEquals(dueBillApiCallNotifier.getCallCount() + getBillCount(), 16, "Bill Data Count Not Matched");
    }

    private DueBillResponse.Bill getBill(int number) {
        DueBillResponse.Bill bill = new DueBillResponse.Bill();
        bill.billNo = bills[number];
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DATE, 1 * number);
        bill.billDueDate = new Timestamp(calendar.getTimeInMillis());
        bill.amount = number * 100.0;
        bill.vat = number * 50.0;
        bill.serviceCharge = number * 5.0;
        bill.detail = new HashMap() {{
            put("ABC", 50);
            put("PQR", 30);
            put("XYZ", 90);
        }};
        return bill;
    }

    @BeforeMethod(onlyForGroups = "caseNewBillNUpdateBill")
    public void caseNewBillNUpdateInit() throws SQLException, JsonProcessingException {
        System.out.println("Preparing All Case Bill Create N Update Data");
        try(Connection connection = CbpDbConnectionPool.getConnection(); Statement statement = connection.createStatement()) {
            System.out.println("Adding bill detail task to generate 1 postpaid billdata");
            String query = "INSERT INTO `bill_detail_task` (`added_by_id`, `status`, `company_id`, `account_no`, `client_id`, `node_id`, `category_id`) VALUES ((select id from user where name = '" + userClient1 + "'), 'Pending', (select id from company where code = '" + company1 + "'), '" + account1 + "', (select id from client where name = '" + client1 + "'), '" + nodeId + "', (select id from category where code = '" + category1 + "'))";
            statement.executeUpdate(query);
            query = "INSERT INTO `bill_detail_task_client_divisions` (`bill_detail_task_id`, `client_divisions_id`) VALUES (last_insert_id(), (select id from client_division where name = '" + division1 + "'))";
            statement.executeUpdate(query);
            System.out.println("Adding bill detail task to generate 1 prepaid billdata");
            query = "INSERT INTO `bill_detail_task` (`added_by_id`, `status`, `client_id`, `node_id`, `category_id`) VALUES ((select id from user where name = '" + userClient1 + "'), 'Pending', (select id from client where name = '" + client1 + "'), '" + nodeId + "', (select id from category where code = '" + category2 + "'))";
            statement.executeUpdate(query);

            System.out.println("Add bills one paid, one to make obsolete, another one to update, another one to existing prepaid unpaid, another one existing paid");
            query = "INSERT INTO `bill` (`client_division_id`, `due_date`, `company_id`, `account_no`, `bill_amount`, `service_charge`, `vat`, `sync_date`, `status`, `bill_no`, `bill_data_id`, `msisdn`) VALUES ((select id from client_division where name = '" + division1 + "'), '2020-12-20 00:00:00', (select id from company where code = '" + company1 + "'), '" + account1 + "', 5000, 5.3, 52, '2020-09-17 11:35:25', '" + BillStatus.Success + "', '444444444444444', (select id from bill_data where company_id = (select id from company where code = '" + company1 + "') and account_no = '" + account1 + "' and client_division_id = (select id from client_division where name = '" + division1 + "')), 1711098361)";
            statement.executeUpdate(query);
            query = "INSERT INTO `bill` (`client_division_id`, `due_date`, `company_id`, `account_no`, `bill_amount`, `service_charge`, `vat`, `sync_date`, `status`, `bill_no`, `bill_data_id`, `msisdn`) VALUES ((select id from client_division where name = '" + division1 + "'), '2020-12-20 00:00:00', (select id from company where code = '" + company1 + "'), '" + account1 + "', 5000, 5.3, 52, '2020-09-17 11:35:25', '" + BillStatus.Fail + "', '555555555555555', (select id from bill_data where company_id = (select id from company where code = '" + company1 + "') and account_no = '" + account1 + "' and client_division_id = (select id from client_division where name = '" + division1 + "')), 1711098361)";
            statement.executeUpdate(query);
            query = "INSERT INTO `bill` (`client_division_id`, `due_date`, `company_id`, `account_no`, `bill_amount`, `service_charge`, `vat`, `sync_date`, `status`, `bill_no`, `bill_data_id`, `msisdn`) VALUES ((select id from client_division where name = '" + division1 + "'), '2020-12-20 00:00:00', (select id from company where code = '" + company1 + "'), '" + account1 + "', 5000, 5.3, 52, '2020-09-17 11:35:25', '" + BillStatus.InProcess + "', '66666666666666', (select id from bill_data where company_id = (select id from company where code = '" + company1 + "') and account_no = '" + account1 + "' and client_division_id = (select id from client_division where name = '" + division1 + "')), 1711098361)";
            statement.executeUpdate(query);
            query = "INSERT INTO `bill` (`client_division_id`, `due_date`, `company_id`, `account_no`, `bill_amount`, `service_charge`, `vat`, `sync_date`, `status`, `bill_no`, `bill_data_id`, `msisdn`) VALUES ((select id from client_division where name = '" + division1 + "'), '2020-12-20 00:00:00', (select id from company where code = '" + company1 + "'), '" + account1 + "', 5000, 5.3, 52, '2020-09-17 11:35:25', '" + BillStatus.Obsolete + "', '" + bills[1] + "', (select id from bill_data where company_id = (select id from company where code = '" + company1 + "') and account_no = '" + account1 + "' and client_division_id = (select id from client_division where name = '" + division1 + "')), 1711098361)";
            statement.executeUpdate(query);
            query = "INSERT INTO `bill` (`client_division_id`, `due_date`, `company_id`, `account_no`, `bill_amount`, `service_charge`, `vat`, `sync_date`, `status`, `bill_no`, `bill_data_id`, `msisdn`) VALUES ((select id from client_division where name = '" + division1 + "'), '2020-12-20 00:00:00', (select id from company where code = '" + company1 + "'), '" + account1 + "', 5000, 5.3, 52, '2020-09-17 11:35:25', '" + BillStatus.Dispute + "', '888888888888', (select id from bill_data where company_id = (select id from company where code = '" + company1 + "') and account_no = '" + account1 + "' and client_division_id = (select id from client_division where name = '" + division1 + "')), 1711098361)";
            statement.executeUpdate(query);
            query = "INSERT INTO `bill` (`client_division_id`, `due_date`, `company_id`, `account_no`, `bill_amount`, `service_charge`, `vat`, `sync_date`, `status`, `bill_no`, `bill_data_id`, `msisdn`) VALUES ((select id from client_division where name = '" + division1 + "'), '2020-12-20 00:00:00', (select id from company where code = '" + company1 + "'), '" + account1 + "', 5000, 5.3, 52, '2020-09-17 11:35:25', '" + BillStatus.Unpaid + "', '" + bills[0] + "', (select id from bill_data where company_id = (select id from company where code = '" + company1 + "') and account_no = '" + account1 + "' and client_division_id = (select id from client_division where name = '" + division1 + "')), 1711098361)";
            statement.executeUpdate(query);

            query = "INSERT INTO `bill` (`client_division_id`, `due_date`, `company_id`, `account_no`, `bill_amount`, `service_charge`, `vat`, `sync_date`, `status`, `bill_no`, `bill_data_id`, `msisdn`) VALUES ((select id from client_division where name = '" + division1 + "'), '2020-12-20 00:00:00', (select id from company where code = '" + company3 + "'), '" + account1 + "', 5000, 5.3, 52, '2020-09-17 11:35:25', '" + BillStatus.Success + "', '444444444444444', (select id from bill_data where company_id = (select id from company where code = '" + company3 + "') and account_no = '" + account1 + "' and client_division_id = (select id from client_division where name = '" + division1 + "')), 1711098361)";
            statement.executeUpdate(query);
            query = "INSERT INTO `bill` (`client_division_id`, `due_date`, `company_id`, `account_no`, `bill_amount`, `service_charge`, `vat`, `sync_date`, `status`, `bill_no`, `bill_data_id`, `msisdn`) VALUES ((select id from client_division where name = '" + division1 + "'), '2020-12-20 00:00:00', (select id from company where code = '" + company3 + "'), '" + account2 + "', 5000, 5.3, 52, '2020-09-17 11:35:25', '" + BillStatus.Fail + "', '555555555555555', (select id from bill_data where company_id = (select id from company where code = '" + company3 + "') and account_no = '" + account2 + "' and client_division_id = (select id from client_division where name = '" + division1 + "')), 1711098361)";
            statement.executeUpdate(query);
            query = "INSERT INTO `bill` (`client_division_id`, `due_date`, `company_id`, `account_no`, `bill_amount`, `service_charge`, `vat`, `sync_date`, `status`, `bill_no`, `bill_data_id`, `msisdn`) VALUES ((select id from client_division where name = '" + division1 + "'), '2020-12-20 00:00:00', (select id from company where code = '" + company4 + "'), '" + account1 + "', 5000, 5.3, 52, '2020-09-17 11:35:25', '" + BillStatus.InProcess + "', '66666666666666', (select id from bill_data where company_id = (select id from company where code = '" + company4 + "') and account_no = '" + account1 + "' and client_division_id = (select id from client_division where name = '" + division1 + "')), 1711098361)";
            statement.executeUpdate(query);
            query = "INSERT INTO `bill` (`client_division_id`, `due_date`, `company_id`, `account_no`, `bill_amount`, `service_charge`, `vat`, `sync_date`, `status`, `bill_no`, `bill_data_id`, `msisdn`) VALUES ((select id from client_division where name = '" + division1 + "'), '2020-12-20 00:00:00', (select id from company where code = '" + company4 + "'), '" + account2 + "', 5000, 5.3, 52, '2020-09-17 11:35:25', '" + BillStatus.Dispute + "', '888888888888', (select id from bill_data where company_id = (select id from company where code = '" + company4 + "') and account_no = '" + account2 + "' and client_division_id = (select id from client_division where name = '" + division1 + "')), 1711098361)";
            statement.executeUpdate(query);
        }

        System.out.println("Mocking due bill api to respond 3 bills");
        dueBillMock.response(200, JsonUtil.toJson(new DueBillResponse() {{
            response = new Response() {{
                company = company1;
                consumerId = account1;
                bills = new ArrayList<>() {{
                    add(getBill(0));
                    add(getBill(1));
                    add(getBill(2));
                }};
            }};
        }}));
    }

    @Test(groups = "caseNewBillNUpdateBill")
    /**
     * Generate New Bills for Postpaid and Prepaid and Update if exists for Postpaid
     */
    public void billCollectionCaseNewBillNUpdateBill() throws SQLException {
        System.out.println("Started Test: Bill Collection Case Bill Create N Update");
        new BillCollector().run();
        Assert.assertEquals(getBillCount(BillStatus.Success), 2, "Success Bill Count Not Matched");
        Assert.assertEquals(getBillCount(BillStatus.Dispute), 2, "Dispute Bill Count Not Matched");
        Assert.assertEquals(getBillCount(BillStatus.InProcess), 2, "InProcess Bill Count Not Matched");
        Assert.assertEquals(getBillCount(BillStatus.Obsolete), 1, "Obsolete Bill Count Not Matched");
        Assert.assertEquals(getBillCount(BillStatus.Fail), 1, "Fail Bill Count Not Matched");
        Assert.assertEquals(getBillCount(BillStatus.Unpaid), 8, "Unpaid Bill Count Not Matched");
    }

    @AfterMethod(onlyForGroups = {"caseNewBillNUpdateBill", "caseRevertibles"})
    public void resetDueBillMock() {
        dueBillMock.response(200, "OK");
    }

    @AfterMethod
    public void clearUpSpringSession() throws SQLException {
        Main.closeSession();
    }

    @AfterMethod(onlyForGroups = {"case9", "case10", "case11", "case12", "caseMulti", "caseNewBillNUpdateBill", "caseRevertibles"})
    public void order2ClearUpBills() throws SQLException {
        System.out.println("Clearing Bills");
        try(Connection connection = CbpDbConnectionPool.getConnection(); Statement statement = connection.createStatement()) {
            String query = "delete from `bill`";
            statement.executeUpdate(query);
        }
    }

    @BeforeMethod(onlyForGroups = "caseRevertibles")
    public void caseRevertibles() throws SQLException, JsonProcessingException {
        System.out.println("Preparing All Case Revertibles Data");
        try(Connection connection = CbpDbConnectionPool.getConnection(); Statement statement = connection.createStatement()) {
            String query = "INSERT INTO `company` (`category_id`, `code`, `name`, `has_bill`, `bill_revertibles`) select id, '" + company11 + "', 'Case With Revertibles', b'1', 'ABC,XYZ' from category where code = '" + category1 + "'";
            statement.executeUpdate(query);
            statement.executeUpdate(getBillDataInsertionQuery(division1, userClient1, company11, account1));
            query = "INSERT INTO `bill_detail_task` (`added_by_id`, `status`, `company_id`, `account_no`, `client_id`, `node_id`, `category_id`) VALUES ((select id from user where name = '" + userClient1 + "'), 'Pending', (select id from company where code = '" + company11 + "'), '" + account1 + "', (select id from client where name = '" + client1 + "'), '" + nodeId + "', (select id from category where code = '" + category1 + "'))";
            statement.executeUpdate(query);
            query = "INSERT INTO `bill_detail_task_client_divisions` (`bill_detail_task_id`, `client_divisions_id`) VALUES (last_insert_id(), (select id from client_division where name = '" + division1 + "'))";
            statement.executeUpdate(query);
        }

        System.out.println("Mocking due bill api to respond revertibles");
        dueBillMock.response(200, JsonUtil.toJson(new DueBillResponse() {{
            response = new Response() {{
                company = company1;
                consumerId = account1;
                bills = new ArrayList<>() {{
                    add(getBill(3));
                }};
            }};
        }}));
    }

    @Test(groups = "caseRevertibles")
    /**
     * Bill with Revertibles
     */
    public void billCollectionCaseRevertibles() throws SQLException {
        System.out.println("Started Test: Bill Collection Case With Revertibles");
        new BillCollector().run();
        Assert.assertEquals(getRevertiblesCount(), 1, "Revertibles Count Not Matched");
    }

    @AfterMethod(onlyForGroups = "caseRevertibles")
    public void order1ClearUpRevertibles() throws SQLException {
        System.out.println("Clearing Revertibles");
        try(Connection connection = CbpDbConnectionPool.getConnection(); Statement statement = connection.createStatement()) {
            String query = "truncate bill_revertible_cache";
            statement.executeUpdate(query);
        }
    }

    @AfterMethod
    public void order3ClearUpTasks() throws SQLException {
        System.out.println("Clearing Bill Detail Task");
        try(Connection connection = CbpDbConnectionPool.getConnection(); Statement statement = connection.createStatement()) {
            String query = "delete from `bill_detail_task_client_divisions`";
            statement.executeUpdate(query);
            query = "delete from `bill_detail_task`";
            statement.executeUpdate(query);
        }
    }

    @AfterClass
    public void order4ClearUpBillData() throws SQLException {
        System.out.println("Clearing Bill Data");
        try(Connection connection = CbpDbConnectionPool.getConnection(); Statement statement = connection.createStatement()) {
            String query = "truncate `bill_data`";
            statement.executeUpdate(query);
        }
    }

    @AfterClass(groups = "caseRevertibles")
    public void order5ClearUpCompanies() throws SQLException {
        System.out.println("Clearing Companies and Revertibles");
        try(Connection connection = CbpDbConnectionPool.getConnection(); Statement statement = connection.createStatement()) {
            String query = "delete from company where name like 'Case %'";
            statement.executeUpdate(query);
        }
    }
}