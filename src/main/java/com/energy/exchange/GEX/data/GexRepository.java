package com.energy.exchange.GEX.data;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.tomcat.jdbc.pool.DataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

import com.energy.exchange.GEX.entity.ApproveRegistrationResponse;
import com.energy.exchange.GEX.entity.BidData;
import com.energy.exchange.GEX.entity.BidDetails;
import com.energy.exchange.GEX.entity.BidInfoDto;
import com.energy.exchange.GEX.entity.Contract;
import com.energy.exchange.GEX.entity.Member;
import com.energy.exchange.GEX.entity.RegistrationRequest;
import com.energy.exchange.GEX.entity.SaveRegistrationRequestStatus;
import com.energy.exchange.GEX.entity.Wallet;
import com.energy.exchange.GEX.entity.WalletTransaction;
import com.energy.exchange.GEX.util.Utility;

@Repository
public class GexRepository {

	@Autowired
	@Qualifier("GEPXDataSource")
	private DataSource gepxDataSrc;

	@Autowired
	private Utility utility;

	private static final Log log = LogFactory.getLog(GexRepository.class);

	public List<RegistrationRequest> getAllRegistrationRequests(Integer refId) throws SQLException {
		List<RegistrationRequest> regReqList = new ArrayList<>();

		Connection conn = null;
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		String query = null;
		if (refId != null) {
			query = "select * from member_registration_request where reg_req_id=?";
		} else {
			query = "select * from member_registration_request";
		}
		try {
			conn = gepxDataSrc.getConnection();
			pstmt = conn.prepareStatement(query);
			if (refId != null) {
				pstmt.setInt(1, refId);
			}
			rs = pstmt.executeQuery();
			while (rs.next()) {
				RegistrationRequest regReq = new RegistrationRequest();
				regReq.setAddress(rs.getString("address"));
				regReq.setDesignation(rs.getString("designation"));
				regReq.setEmailId(rs.getString("email_id"));
				regReq.setId(rs.getInt("reg_req_id"));
				regReq.setLocation(rs.getString("location"));
				regReq.setMobileNumber(rs.getString("contact_number"));
				regReq.setName(rs.getString("name"));
				regReq.setOrgName(rs.getString("org_name"));
				regReq.setStatus(rs.getString("status"));
				regReqList.add(regReq);
			}
		} catch (SQLException e) {
			throw new SQLException("SQL Exception in getAllRegistrationRequests method " + e.getMessage());
		} finally {
			Utility.closeConnection(conn, pstmt, rs);
		}
		return regReqList;
	}

	public SaveRegistrationRequestStatus saveRegistrationRequests(RegistrationRequest req) throws SQLException {
		SaveRegistrationRequestStatus status = new SaveRegistrationRequestStatus();
		Connection conn = null;
		PreparedStatement pstmt = null;
		String query = "insert into member_registration_request(email_id, org_name,name, location,address,contact_number,designation,status)"
				+ " values(?,?,?,?,?,?,?,?)";

		try {
			conn = gepxDataSrc.getConnection();
			pstmt = conn.prepareStatement(query);
			pstmt.setString(1, req.getEmailId());
			pstmt.setString(2, req.getOrgName());
			pstmt.setString(3, req.getName());
			pstmt.setString(4, req.getLocation());
			pstmt.setString(5, req.getAddress());
			pstmt.setString(6, req.getMobileNumber());
			pstmt.setString(7, req.getDesignation());
			pstmt.setString(8, "Pending"); // set status as Pending
			pstmt.executeUpdate();

			status.setError(null);
			status.setMsg("success");
			status.setRefId(getReferenceId());

		} catch (SQLException e) {
			status.setError("DB Error");
			status.setMsg(e.getMessage());
			status.setRefId(null);
			log.error("SQL Exception in saveRegistrationRequests method " + e.getMessage());
		} finally {
			Utility.closeConnection(conn, pstmt, null);

		}
		return status;
	}

	public Integer getReferenceId() throws SQLException {
		Integer refId = null;
		Connection conn = null;
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		String query = "select max(reg_req_id) as refId from member_registration_request";
		try {
			conn = gepxDataSrc.getConnection();
			pstmt = conn.prepareStatement(query);
			rs = pstmt.executeQuery();
			while (rs.next()) {
				refId = rs.getInt("refId");
			}
		} catch (SQLException e) {
			throw new SQLException("SQL Exception in getAllRegistrationRequests method " + e.getMessage());

		} finally {
			Utility.closeConnection(conn, pstmt, rs);

		}

		return refId;
	}

	public ApproveRegistrationResponse approveRegistrationRequests(Integer refId) throws SQLException {
		ApproveRegistrationResponse res = new ApproveRegistrationResponse();
		List<RegistrationRequest> req = getAllRegistrationRequests(refId);
		if (req.isEmpty()) {
			res.setMsg("Failed");
			res.setError("No request is available for the given reference id");
			return res;
		}
		int orgId = createOrganization(req.get(0).getOrgName());
		if (orgId == -1) {
			res.setMsg("Failed");
			res.setError("Error creating organization");
			return res;
		}
		String password = utility.generateRandomPassword(10);
		String encrptPassword = utility.encryptString(password);
		Connection conn = null;
		PreparedStatement pstmt = null;
		String query = "insert into gepx.member(email_id,name,role_id,org_id,location,designation,address,password) \r\n"
				+ "values(?,?,2,?,?,?,?,?);";
		try {
			conn = gepxDataSrc.getConnection();
			pstmt = conn.prepareStatement(query);
			pstmt.setString(1, req.get(0).getEmailId());
			pstmt.setString(2, req.get(0).getName());
			pstmt.setInt(3, orgId);
			pstmt.setString(4, req.get(0).getLocation());
			pstmt.setString(5, req.get(0).getDesignation());
			pstmt.setString(6, req.get(0).getAddress());
			pstmt.setString(7, encrptPassword);
			pstmt.executeUpdate();

			res.setMsg("Success");
			res.setUserName(req.get(0).getEmailId());
			res.setPassword(password);
		} catch (SQLException e) {
			throw new SQLException("SQL Exception in approveRegistrationRequests method " + e.getMessage());

		} finally {
			Utility.closeConnection(conn, pstmt, null);

		}
		return res;
	}

	public void updateStatus(Integer refId, String status) throws SQLException {
		Connection conn = null;
		PreparedStatement pstmt = null;
		String query = "update member_registration_request set status=? where reg_req_id=?";
		try {
			conn = gepxDataSrc.getConnection();
			pstmt = conn.prepareStatement(query);
			pstmt.setString(1, status);
			pstmt.setInt(2, refId);
			pstmt.executeUpdate();
		} catch (SQLException e) {
			throw new SQLException("SQL Exception in updateStatus method " + e.getMessage());

		} finally {
			Utility.closeConnection(conn, pstmt, null);

		}
	}

	private Integer createOrganization(String orgName) throws SQLException {
		Connection conn = null;
		PreparedStatement pstmt = null;
		ResultSet tableKeys = null;
		int autoGeneratedID = -1;
		String query = "insert into gepx.org(org_name) values(?)";
		String walQuery = "insert into member_wallet(org_id,total_amount,traded_amount) values(?,0,0)";
		try {
			conn = gepxDataSrc.getConnection();
			pstmt = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
			pstmt.setString(1, orgName);
			pstmt.executeUpdate();
			tableKeys = pstmt.getGeneratedKeys();
			tableKeys.next();
			autoGeneratedID = tableKeys.getInt(1);
			
			//Intialize wallet account
			pstmt.close();
			pstmt = conn.prepareStatement(walQuery);
			pstmt.setInt(1, autoGeneratedID);
			pstmt.executeUpdate();
		} catch (SQLException e) {
			throw new SQLException("SQL Exception in createOrganization method " + e.getMessage());

		} finally {
			Utility.closeConnection(conn, pstmt, tableKeys);

		}
		return autoGeneratedID;
	}

	public List<BidData> getAggregatedBidDetails(String date, int timeBlock, String bidType, String bidStatus) throws SQLException {
		List<BidData> res = new ArrayList<>();
		Connection conn = null;
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		String query = "select price,sum(quantity) as sumQty from bid_info where dateTime=? and time_block=? and bid_type=? and bid_status=? group by price order by price";
		try {
			conn = gepxDataSrc.getConnection();
			pstmt = conn.prepareStatement(query);
			pstmt.setString(1, date);
			pstmt.setInt(2, timeBlock);
			pstmt.setString(3, bidType);
			pstmt.setString(4, bidStatus);
			rs = pstmt.executeQuery();
			while (rs.next()) {
				BidData data = new BidData();
				data.setPrice(rs.getDouble("price"));
				data.setQuantity(rs.getInt("sumQty"));
				res.add(data);
			}
		} catch (SQLException e) {
			throw new SQLException("SQL Exception in getAggregatedBidDetails method " + e.getMessage());

		} finally {
			Utility.closeConnection(conn, pstmt, rs);

		}
		return res;
	}

	public Member getMemberDetailsForLogin(String userName) throws SQLException {
		Member res = null;
		Connection conn = null;
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		String query = "select member_id,password from gepx.member where email_id=?";
		try {
			conn = gepxDataSrc.getConnection();
			pstmt = conn.prepareStatement(query);
			pstmt.setString(1, userName);
			rs = pstmt.executeQuery();
			if (rs.next()) {
				res = new Member();
				res.setMemberId(rs.getInt("member_id"));
				res.setEmailId(userName);
				res.setPassword(rs.getString("password"));
			}
		} catch (SQLException e) {
			throw new SQLException("SQL Exception in getMemberDetails method " + e.getMessage());

		} finally {
			Utility.closeConnection(conn, pstmt, rs);

		}
		return res;
	}
	
	public Member getMemberDetails(int membId) throws SQLException {
		Member res = null;
		Connection conn = null;
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		String query = "select * from gepx.member where member_id=?";
		try {
			conn = gepxDataSrc.getConnection();
			pstmt = conn.prepareStatement(query);
			pstmt.setInt(1, membId);
			rs = pstmt.executeQuery();
			if (rs.next()) {
				res = new Member();
				res.setMemberId(rs.getInt("member_id"));
				res.setEmailId((rs.getString("email_id")));
				res.setOrg_id(rs.getInt("org_id"));
			}
		} catch (SQLException e) {
			throw new SQLException("SQL Exception in getMemberDetails method " + e.getMessage());

		} finally {
			Utility.closeConnection(conn, pstmt, rs);

		}
		return res;
	}
	
	public void saveBidInfo(BidInfoDto bidInfoDto) throws SQLException
	{
		Connection conn = null;
		PreparedStatement pstmt = null;
		String query = "insert into bid_info (member_id, time_block, DateTime, bid_type, quantity, price, bid_status) values(?,?, curdate(), ?, ?, ?, ?)";
		
		try {
			conn = gepxDataSrc.getConnection();
			pstmt = conn.prepareStatement(query);
			Map<Integer, List<BidData>> bidData=bidInfoDto.getBidInfo();
			for(Map.Entry<Integer, List<BidData>> entry: bidData.entrySet())
			{
				for(int i=0;i<entry.getValue().size();i++)
				{
					BidData bidInfo = entry.getValue().get(i);

					pstmt.setInt(1, bidInfoDto.getMemberId()); //member_id
					pstmt.setInt(2, entry.getKey()); //time_block
					String bidType="";
					if("sell".equalsIgnoreCase(bidInfo.getAction()))
					{
						bidType = "S";
					}	
					else if("buy".equalsIgnoreCase(bidInfo.getAction()))
					{
						bidType = "B";			
					}
					pstmt.setString(3, bidType);
					pstmt.setInt(4, bidInfo.getQuantity()); //quantity
					pstmt.setDouble(5, bidInfo.getPrice()); //price
					pstmt.setString(6, "Pending");
					pstmt.addBatch();

				}
			}
			pstmt.executeBatch();

		} catch (SQLException e) {
			
			throw new SQLException("SQL Exception in saveBidInfo method " + e.getMessage());
			
		} finally {
			Utility.closeConnection(conn, pstmt, null);

		}
		
	}
	
	public List<BidDetails> getAllBidDetails(String date, int timeBlock, String bidStatus, String sellConfirm) throws SQLException {
		List<BidDetails> res = new ArrayList<>();
		Connection conn = null;
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		String buyBids = "select * from bid_info where dateTime=? and time_block=? and bid_type='B' and bid_status=?";
		StringBuilder sellBids = new StringBuilder("select * from bid_info where dateTime=? and time_block=? and bid_status=? and bid_type='S'");
		if(sellConfirm!=null) {
			sellBids.append(" and seller_confirmation=?");
		}
		
		try {
			conn = gepxDataSrc.getConnection();
			pstmt = conn.prepareStatement(buyBids);
			pstmt.setString(1, date);
			pstmt.setInt(2, timeBlock);
			pstmt.setString(3, bidStatus);
			rs = pstmt.executeQuery();
			while (rs.next()) {
				BidDetails data = new BidDetails();
				data.setPrice(rs.getDouble("price"));
				data.setQuantity(rs.getInt("quantity"));
				data.setBidId(rs.getInt("bid_id"));
				data.setMemberId(rs.getInt("member_id"));
				data.setBidType(rs.getString("bid_type"));
				data.setBidStatus(rs.getString("bid_status"));
				res.add(data);
			}
			pstmt.close();
			rs.close();
			pstmt = conn.prepareStatement(sellBids.toString());
			pstmt.setString(1, date);
			pstmt.setInt(2, timeBlock);
			pstmt.setString(3, bidStatus);
			if(sellConfirm!=null) {
				pstmt.setString(4, sellConfirm);
			}
			rs = pstmt.executeQuery();
			while (rs.next()) {
				BidDetails data = new BidDetails();
				data.setPrice(rs.getDouble("price"));
				data.setQuantity(rs.getInt("quantity"));
				data.setBidId(rs.getInt("bid_id"));
				data.setMemberId(rs.getInt("member_id"));
				data.setBidType(rs.getString("bid_type"));
				data.setBidStatus(rs.getString("bid_status"));
				res.add(data);
			}
		} catch (SQLException e) {
			throw new SQLException("SQL Exception in getAllBidDetails method " + e.getMessage());

		} finally {
			Utility.closeConnection(conn, pstmt, rs);

		}
		return res;
	}
	
	public List<BidDetails> getAllBidDetailsForContract(String date, int timeBlock, String bidStatus, String bidType) throws SQLException {
		List<BidDetails> res = new ArrayList<>();
		Connection conn = null;
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		StringBuilder sellBids = new StringBuilder("select member_id,sum(quantity) as qty from bid_info where dateTime=? and time_block=? and bid_status=? and bid_type=?"
				+ "group by member_id order by qty desc");
		
		try {
			conn = gepxDataSrc.getConnection();
			pstmt = conn.prepareStatement(sellBids.toString());
			pstmt.setString(1, date);
			pstmt.setInt(2, timeBlock);
			pstmt.setString(3, bidStatus);
			pstmt.setString(4, bidType);
			rs = pstmt.executeQuery();
			while (rs.next()) {
				BidDetails data = new BidDetails();
				//data.setPrice(rs.getDouble("price"));
				data.setQuantity(rs.getInt("qty"));
				//data.setBidId(rs.getInt("bid_id"));
				data.setMemberId(rs.getInt("member_id"));
				//data.setBidType(rs.getString("bid_type"));
				//data.setBidStatus(rs.getString("bid_status"));
				res.add(data);
			}
		} catch (SQLException e) {
			throw new SQLException("SQL Exception in getAllBidDetailsForContract method " + e.getMessage());

		} finally {
			Utility.closeConnection(conn, pstmt, rs);

		}
		return res;
	}
	
	
	
	public int updateWallet(int org_id, double amount) throws SQLException {
		int response=0;
		Connection conn = null;
		PreparedStatement pstmt = null;
		String query = "update member_wallet set total_amount=total_amount+? where org_id=?";
		try {
			conn = gepxDataSrc.getConnection();
			pstmt = conn.prepareStatement(query);
			pstmt.setDouble(1, amount);
			pstmt.setInt(2, org_id);
			response = pstmt.executeUpdate();
		} catch (SQLException e) {
			throw new SQLException("SQL Exception in updateWallet method " + e.getMessage());

		} finally {
			Utility.closeConnection(conn, pstmt, null);

		}
		return response;
	}
	
	public int updateWalletForContract(int memberId, double amount) throws SQLException {
		int response=0;
		Connection conn = null;
		PreparedStatement pstmt = null;
		String query = "update member_wallet set total_amount=total_amount-? where org_id=(select org_id from gepx.member where member_id=?)";
		try {
			conn = gepxDataSrc.getConnection();
			pstmt = conn.prepareStatement(query);
			pstmt.setDouble(1, amount);
			pstmt.setInt(2, memberId);
			response = pstmt.executeUpdate();
			pstmt.close();
			query = "update member_wallet set traded_amount=traded_amount+? where org_id=(select org_id from gepx.member where member_id=?)";
			pstmt = conn.prepareStatement(query);
			pstmt.setDouble(1, amount);
			pstmt.setInt(2, memberId);
			response = pstmt.executeUpdate();
		} catch (SQLException e) {
			throw new SQLException("SQL Exception in updateWallet method " + e.getMessage());

		} finally {
			Utility.closeConnection(conn, pstmt, null);

		}
		return response;
	}

	public int addWalletTransaction(Member member,double amount,String trans_type,String trans_mode,String status) throws SQLException {
		Connection conn = null;
		PreparedStatement pstmt = null;
		ResultSet tableKeys = null;
		int autoGeneratedID = -1;
		String query = "insert into gepx.wallet_transaction(member_id,org_id,amount,transaction_type,transaction_mode,status) values(?,?,?,?,?,?)";
		try {
			conn = gepxDataSrc.getConnection();
			pstmt = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
			pstmt.setInt(1, member.getMemberId());
			pstmt.setInt(2, member.getOrg_id());
			pstmt.setDouble(3, amount);
			pstmt.setString(4, trans_type);
			pstmt.setString(5, trans_mode);
			pstmt.setString(6, status);
			pstmt.executeUpdate();
			tableKeys = pstmt.getGeneratedKeys();
			tableKeys.next();
			autoGeneratedID = tableKeys.getInt(1);
		} catch (SQLException e) {
			throw new SQLException("SQL Exception in addWalletTransaction method " + e.getMessage());

		} finally {
			Utility.closeConnection(conn, pstmt, tableKeys);

		}
		return autoGeneratedID;
	}
	
	public List<WalletTransaction> getWalletTransactions(int memberId) throws SQLException {
		List<WalletTransaction> history=new ArrayList<>();
		Connection conn = null;
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		String query = null;
		
		query = "select * from gepx.wallet_transaction where org_id=(select org_id from gepx.member where member_id=?)";
		try {
			conn = gepxDataSrc.getConnection();
			pstmt = conn.prepareStatement(query);
			pstmt.setInt(1, memberId);
			rs = pstmt.executeQuery();
			while (rs.next()) {
				WalletTransaction trans = new WalletTransaction();
				trans.setId(rs.getInt("transaction_id"));
				trans.setAmount(rs.getDouble("amount"));
				Date tdate =rs.getTimestamp("dateTime");
				String formattedDate=new SimpleDateFormat("yyyy/MM/dd HH:mm:ss").format(tdate);
				trans.setDate(formattedDate);
				trans.setTransaction(rs.getString("transaction_type"));
				trans.setStatus(rs.getString("status"));
				trans.setTransMode(rs.getString("transaction_mode"));
				history.add(trans);
			}
		} catch (SQLException e) {
			throw new SQLException("SQL Exception in getWalletTransactions method " + e.getMessage());
		} finally {
			Utility.closeConnection(conn, pstmt, rs);
		}
		return history;
	}
	
	public Wallet getWalletDetails(int memberId) throws SQLException {
		Wallet resp = new Wallet();
		Connection conn = null;
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		String query = null;
		
		query = "select * from gepx.member_wallet where org_id=(select org_id from gepx.member where member_id=?)";
		try {
			conn = gepxDataSrc.getConnection();
			pstmt = conn.prepareStatement(query);
			pstmt.setInt(1, memberId);
			rs = pstmt.executeQuery();
			if (rs.next()) {
				resp.setWallet_id(rs.getInt("wallet_id"));
				resp.setTotalAmount(rs.getDouble("total_amount"));
				resp.setTradedAmount(rs.getDouble("traded_amount"));
			}
		} catch (SQLException e) {
			throw new SQLException("SQL Exception in getWalletDetails method " + e.getMessage());
		} finally {
			Utility.closeConnection(conn, pstmt, rs);
		}
		return resp;
	}
	
	public void updateBidStatus(int bidId, String bidStatus) throws SQLException {
		Connection conn = null;
		PreparedStatement pstmt = null;
		String query = "update bid_info set bid_status=? where bid_id=?";
		try {
			conn = gepxDataSrc.getConnection();
			pstmt = conn.prepareStatement(query);
			pstmt.setString(1, bidStatus);
			pstmt.setInt(2, bidId);
			pstmt.executeUpdate();
		} catch (SQLException e) {
			throw new SQLException("SQL Exception in updateBidStatus method " + e.getMessage());

		} finally {
			Utility.closeConnection(conn, pstmt, null);
		}
	}
	
	public void comfirmSellBid(int bidId) throws SQLException {
		Connection conn = null;
		PreparedStatement pstmt = null;
		String query = "update bid_info set seller_confirmation=? where bid_id=?";
		try {
			conn = gepxDataSrc.getConnection();
			pstmt = conn.prepareStatement(query);
			pstmt.setString(1, "accepted");
			pstmt.setInt(2, bidId);
			pstmt.executeUpdate();
		} catch (SQLException e) {
			throw new SQLException("SQL Exception in comfirmSellBid method " + e.getMessage());

		} finally {
			Utility.closeConnection(conn, pstmt, null);
		}
	}
	
	public List<BidDetails> getProvisionallyClearedBids(int memberId, String date) throws SQLException {
		List<BidDetails> res = new ArrayList<>();
		Connection conn = null;
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		String query = "select * from bid_info where dateTime=? and member_id=? and bid_status='Provisionally Cleared' and seller_confirmation is null";
		try {
			conn = gepxDataSrc.getConnection();
			pstmt = conn.prepareStatement(query);
			pstmt.setString(1, date);
			pstmt.setInt(2, memberId);
			rs = pstmt.executeQuery();
			while (rs.next()) {
				BidDetails data = new BidDetails();
				data.setPrice(rs.getDouble("price"));
				data.setQuantity(rs.getInt("quantity"));
				data.setBidId(rs.getInt("bid_id"));
				data.setMemberId(rs.getInt("member_id"));
				data.setBidType(rs.getString("bid_type"));
				data.setBidStatus(rs.getString("bid_status"));
				data.setTimeSlot(rs.getInt("time_block"));
				res.add(data);
			}
		} catch (SQLException e) {
			throw new SQLException("SQL Exception in getProvisionallyClearedBids method " + e.getMessage());

		} finally {
			Utility.closeConnection(conn, pstmt, rs);
		}
		return res;
	}
	
	public void storeContractsInDB(List<Contract> contractsList) throws SQLException {
		Connection conn = null;
		PreparedStatement pstmt = null;
		String query = "insert into gepx.contract(dateTime,time_block,buyer_id,seller_id,price,quantity)"
				+ "values(curdate(),?,?,?,?,?)";
		
		try {
			conn = gepxDataSrc.getConnection();
			pstmt = conn.prepareStatement(query);
			
			for(Contract cont: contractsList) {

				pstmt.setInt(1, cont.getTimeBlock());
				pstmt.setInt(2, cont.getBuyer()); 
				pstmt.setInt(3, cont.getSeller()); 
				pstmt.setDouble(4, cont.getPrice()); 
				pstmt.setInt(5, cont.getQuantity()); 
				pstmt.addBatch();
			}
			pstmt.executeBatch();

		} catch (SQLException e) {
			
			throw new SQLException("SQL Exception in storeContractsInDB method " + e.getMessage());
			
		} finally {
			Utility.closeConnection(conn, pstmt, null);

		}
	}
	
	public void saveMCPandMCVinDB(String date,int timeSlot,BidData data) throws SQLException {
		Connection conn = null;
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		int auctionId=-1;
		String query = "select auction_id from gepx.auction where dateTime=? and time_block=?";
		try {
			conn = gepxDataSrc.getConnection();
			pstmt = conn.prepareStatement(query);
			pstmt.setString(1, date);
			pstmt.setInt(2, timeSlot);
			rs = pstmt.executeQuery();
			if (rs.next()) {
				auctionId=rs.getInt("auction_id");
			} 
			pstmt.close();
			if(auctionId!=-1) {
				query = "update gepx.auction set mcp=?,mcv=? where auction_id=?";
				pstmt = conn.prepareStatement(query);
				pstmt.setDouble(1, data.getPrice());
				pstmt.setInt(2, data.getQuantity());
				pstmt.setInt(3, auctionId);
				pstmt.executeUpdate();
			} else {
				query = "insert into gepx.auction(dateTime,time_block,mcp,mcv) values(?,?,?,?)";
				pstmt = conn.prepareStatement(query);
				pstmt.setString(1, date);
				pstmt.setInt(2, timeSlot);
				pstmt.setDouble(3, data.getPrice());
				pstmt.setInt(4, data.getQuantity());
				pstmt.executeUpdate();
			}
		} catch (SQLException e) {
			throw new SQLException("SQL Exception in saveMCPandMCVinDB method " + e.getMessage());

		} finally {
			Utility.closeConnection(conn, pstmt, rs);
		}
	}
	
	public List<Contract> getContractDetails(int memberId) throws SQLException {
		List<Contract> contList = new ArrayList<>();
		Connection conn = null;
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		String query = "select contract_id,dateTime,time_block,price,quantity,buyer_id,seller_id,(select org_name from gepx.org where org_id="
				+ "(select org_id from gepx.member where member_id=buyer_id)) as buyerOrg,\r\n" + 
				"(select org_name from gepx.org where org_id=(select org_id from gepx.member where member_id=seller_id)) as sellerOrg\r\n" + 
				"from gepx.contract where buyer_id=? or seller_id=?";
		try {
			conn = gepxDataSrc.getConnection();
			pstmt = conn.prepareStatement(query);
			pstmt.setInt(1, memberId);
			pstmt.setInt(2, memberId);
			rs = pstmt.executeQuery();
			while (rs.next()) {
				Contract contr=new Contract();
				contr.setId(rs.getInt("contract_id"));
				contr.setDate(rs.getString("dateTime"));
				contr.setTimeBlock(rs.getInt("time_block"));
				contr.setBuyer(rs.getInt("buyer_id"));
				contr.setSeller(rs.getInt("seller_id"));
				contr.setPrice(rs.getDouble("price"));
				contr.setQuantity(rs.getInt("quantity"));
				contr.setBuyerOrg(rs.getString("buyerOrg"));
				contr.setSellerOrg(rs.getString("sellerOrg"));
				contList.add(contr);
			}
		} catch (SQLException e) {
			throw new SQLException("SQL Exception in getContractDetails method " + e.getMessage());

		} finally {
			Utility.closeConnection(conn, pstmt, rs);

		}
		return contList;
	}
	
	public BidData getMCPForGivenSlot(int timeSlot, String date) throws SQLException {
		BidData res=new BidData();
		Connection conn = null;
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		String query = "select * from gepx.auction where time_block=? and dateTime=?";
		try {
			conn = gepxDataSrc.getConnection();
			pstmt = conn.prepareStatement(query);
			pstmt.setInt(1, timeSlot);
			pstmt.setString(2, date);
			rs = pstmt.executeQuery();
			if (rs.next()) {
				res.setPrice(rs.getDouble("mcp"));
				res.setQuantity(rs.getInt("mcv"));
			}
		} catch (SQLException e) {
			throw new SQLException("SQL Exception in getMCPForGivenSlot method " + e.getMessage());

		} finally {
			Utility.closeConnection(conn, pstmt, rs);

		}
		return res;
	}

}
