package com.example.assignment;

import com.example.assignment.*;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Mapper utility to convert ResultSet rows to model objects.
 * Centralizes object creation from database queries.
 * Works with the database VIEWS and stored PROCEDURES.
 */
public class DataMapper {

    /**
     * Maps a ResultSet row to a Vehicle object
     */
    public static Vehicle mapVehicle(ResultSet rs) throws SQLException {
        Vehicle v = new Vehicle();
        v.setId(rs.getInt("vehicle_id"));
        v.setRegistrationNumber(rs.getString("registration_number"));
        v.setMake(rs.getString("make"));
        v.setModel(rs.getString("model"));
        v.setYear(rs.getInt("year"));
        v.setColor(rs.getString("color"));
        v.setEngineNumber(rs.getString("engine_number"));
        v.setChassisNumber(rs.getString("chassis_number"));
        v.setOwnerId(rs.getInt("owner_id"));
        v.setRegistrationDate(rs.getString("registration_date"));
        v.setStolen(rs.getBoolean("is_stolen"));
        return v;
    }

    /**
     * Maps a ResultSet row to a Customer object
     */
    public static Customer mapCustomer(ResultSet rs) throws SQLException {
        Customer c = new Customer();
        c.setId(rs.getInt("customer_id"));
        c.setName(rs.getString("name"));
        c.setAddress(rs.getString("address"));
        c.setPhone(rs.getString("phone"));
        c.setEmail(rs.getString("email"));
        c.setIdNumber(rs.getString("id_number"));
        return c;
    }

    /**
     * Maps a ResultSet row to a User object
     */
    public static User mapUser(ResultSet rs) throws SQLException {
        User u = new User();
        u.setId(rs.getInt("user_id"));
        u.setUsername(rs.getString("username"));
        u.setPasswordHash(rs.getString("password_hash"));
        u.setRole(rs.getString("role"));
        u.setName(rs.getString("full_name"));
        u.setEmail(rs.getString("email"));
        u.setPhone(rs.getString("phone"));
        u.setActive(rs.getBoolean("is_active"));
        return u;
    }

    /**
     * Maps a ResultSet row to a ServiceRecord object
     */
    public static ServiceRecord mapServiceRecord(ResultSet rs) throws SQLException {
        ServiceRecord sr = new ServiceRecord();
        sr.setId(rs.getInt("service_id"));
        sr.setVehicleId(rs.getInt("vehicle_id"));
        sr.setRecordDate(rs.getString("service_date"));
        sr.setServiceType(rs.getString("service_type"));
        sr.setDescription(rs.getString("description"));
        sr.setCost(rs.getBigDecimal("cost"));
        sr.setWorkshopName(rs.getString("workshop_name"));
        sr.setMileage(rs.getInt("mileage"));
        sr.setNextServiceDate(rs.getString("next_service_date"));
        return sr;
    }

    /**
     * Maps a ResultSet row to a CustomerQuery object
     */
    public static CustomerQuery mapCustomerQuery(ResultSet rs) throws SQLException {
        CustomerQuery cq = new CustomerQuery();
        cq.setId(rs.getInt("query_id"));
        cq.setCustomerId(rs.getInt("customer_id"));
        cq.setVehicleId(rs.getInt("vehicle_id"));
        cq.setRecordDate(rs.getString("query_date"));
        cq.setQueryText(rs.getString("query_text"));
        cq.setResponseText(rs.getString("response_text"));
        cq.setStatus(rs.getString("status"));
        return cq;
    }

    /**
     * Maps a ResultSet row to a PoliceReport object
     */
    public static PoliceReport mapPoliceReport(ResultSet rs) throws SQLException {
        PoliceReport pr = new PoliceReport();
        pr.setId(rs.getInt("report_id"));
        pr.setVehicleId(rs.getInt("vehicle_id"));
        pr.setRecordDate(rs.getString("report_date"));
        pr.setReportType(rs.getString("report_type"));
        pr.setDescription(rs.getString("description"));
        pr.setOfficerName(rs.getString("officer_name"));
        pr.setStation(rs.getString("station"));
        pr.setCaseNumber(rs.getString("case_number"));
        return pr;
    }

    /**
     * Maps a ResultSet row to a Violation object
     */
    public static Violation mapViolation(ResultSet rs) throws SQLException {
        Violation vl = new Violation();
        vl.setId(rs.getInt("violation_id"));
        vl.setVehicleId(rs.getInt("vehicle_id"));
        vl.setRecordDate(rs.getString("violation_date"));
        vl.setViolationType(rs.getString("violation_type"));
        vl.setFineAmount(rs.getBigDecimal("fine_amount"));
        vl.setStatus(rs.getString("status"));
        vl.setLocation(rs.getString("location"));
        vl.setOfficerName(rs.getString("officer_name"));
        vl.setPaidDate(rs.getString("paid_date"));
        return vl;
    }

    /**
     * Maps a ResultSet row to an InsurancePolicy object
     */
    public static InsurancePolicy mapInsurancePolicy(ResultSet rs) throws SQLException {
        InsurancePolicy ip = new InsurancePolicy();
        ip.setId(rs.getInt("policy_id"));
        ip.setVehicleId(rs.getInt("vehicle_id"));
        ip.setCustomerId(rs.getInt("customer_id"));
        ip.setPolicyNumber(rs.getString("policy_number"));
        ip.setProvider(rs.getString("provider"));
        ip.setCoverageType(rs.getString("coverage_type"));
        ip.setStartDate(rs.getString("start_date"));
        ip.setEndDate(rs.getString("end_date"));
        ip.setPremium(rs.getBigDecimal("premium"));
        ip.setActive(rs.getBoolean("is_active"));
        return ip;
    }
}
