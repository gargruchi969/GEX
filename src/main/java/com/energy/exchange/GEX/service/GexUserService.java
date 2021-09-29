package com.energy.exchange.GEX.service;

import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.energy.exchange.GEX.data.GexRepository;
import com.energy.exchange.GEX.entity.ApproveRegistrationResponse;
import com.energy.exchange.GEX.entity.BidData;
import com.energy.exchange.GEX.entity.BidDetails;
import com.energy.exchange.GEX.entity.BidInfoBlockChain;
import com.energy.exchange.GEX.entity.BidInfoDto;
import com.energy.exchange.GEX.entity.Contract;
import com.energy.exchange.GEX.entity.Member;
import com.energy.exchange.GEX.entity.PlaceBidTransaction;
import com.energy.exchange.GEX.entity.RegistrationRequest;
import com.energy.exchange.GEX.entity.SaveRegistrationRequestStatus;
import com.energy.exchange.GEX.entity.Wallet;
import com.energy.exchange.GEX.entity.WalletTransaction;
import com.energy.exchange.GEX.util.Utility;

@Component
public class GexUserService {

	@Autowired
	GexRepository gexRepo;

	@Autowired
	private Utility utility;

	private static final Log log = LogFactory.getLog(GexUserService.class);

	public List<RegistrationRequest> getRegistrationRequests() throws SQLException {
		return gexRepo.getAllRegistrationRequests(null);
	}

	public SaveRegistrationRequestStatus saveRegistrationRequests(RegistrationRequest req) throws SQLException {
		return gexRepo.saveRegistrationRequests(req);
	}

	public ApproveRegistrationResponse approveRegistrationRequests(Integer refId, String action) {
		ApproveRegistrationResponse res = new ApproveRegistrationResponse();
		try {
			if ("reject".equalsIgnoreCase(action)) {
				gexRepo.updateStatus(refId, "Rejected");
				res.setMsg("Success");
			} else if ("approve".equalsIgnoreCase(action)) {
				res = gexRepo.approveRegistrationRequests(refId);
				if ("Success".equalsIgnoreCase(res.getMsg())) {
					gexRepo.updateStatus(refId, "Approved");
				}
			} else {
				res.setMsg("Failed");
				res.setError("Invalid action");
			}
		} catch (SQLException e) {
			log.error("Error approving registration request: "+e.getMessage());
			res.setMsg(e.getMessage());
			res.setError("DB Error");
		}
		return res;
	}

	public BidData calculateMCP(String date) throws SQLException, ParseException {
		
		List<BidData> dataList=new ArrayList<>();
		BidData result=new BidData();
		BidData sum=new BidData();
		sum.setPrice(new Double(0));
		sum.setQuantity(0);
		
		int count=0;
		for(int i=1;i<=96;i++) {
			BidData temp = calculateMCPForGivenSlot(i,date,"Pending");
			if(null!=temp.getPrice()) {
				gexRepo.saveMCPandMCVinDB(date,i,temp);
				count++;
				dataList.add(temp);
				sum.setPrice(sum.getPrice()+temp.getPrice());
				sum.setQuantity(sum.getQuantity()+temp.getQuantity());
			}
		}
		if(count>0) {
			Double finalMCP=sum.getPrice()/count;
			int finalMCV=sum.getQuantity()/count;
			result.setPrice(finalMCP);
			result.setQuantity(finalMCV);
			gexRepo.saveMCPandMCVinDB(date,0,result);// Storing Average MCP in 0th slot
			log.info("Final MCP && MCV is: "+finalMCP+" && "+finalMCV);
		}
		
		return result;
		
	}
	
	public BidData calculateMCPForGivenSlot(int timeBlock, String date, String bidStatus) throws SQLException, ParseException {
		
		Date formattedDate=new SimpleDateFormat("yyyy-MM-dd").parse(date);
		BidData result = new BidData();
		
		result=gexRepo.getMCPForGivenSlot(timeBlock,date);
		if(result.getPrice()!=null) {
			return result;
		} else {
			List<BidData> demandData = gexRepo.getAggregatedBidDetails(date, timeBlock, "B", bidStatus);
			List<BidData> supplyData = gexRepo.getAggregatedBidDetails(date, timeBlock, "S", bidStatus);
			
			if(!demandData.isEmpty() && !supplyData.isEmpty()) {
				result = matching(demandData,supplyData);
			}
			if(result.getPrice()!=null) {
				gexRepo.saveMCPandMCVinDB(date,timeBlock,result);
				ProvisionallyClearBids(timeBlock,date,result);
			}
		}
		log.info("MCP && MCV for slot "+timeBlock+" is: "+result.getPrice()+" && "+result.getQuantity());
		
		return result;
	}
	
	public void ProvisionallyClearBids(int timeBlock, String date,BidData marketCleared) throws SQLException, ParseException {
		Date formattedDate=new SimpleDateFormat("yyyy-MM-dd").parse(date);
		List<BidDetails> bidList = gexRepo.getAllBidDetails(date, timeBlock, "Pending", null);
		
		if(marketCleared.getPrice()==null) {
			marketCleared= calculateMCPForGivenSlot(timeBlock,date, "Pending");
		}
		
		for(BidDetails bidDetail: bidList) {
			if("B".equals(bidDetail.getBidType())) {
				if(bidDetail.getPrice()>=marketCleared.getPrice() && bidDetail.getQuantity()<=marketCleared.getQuantity()) {
					bidDetail.setBidStatus("Provisionally Cleared");
				} else {
					bidDetail.setBidStatus("Rejected");
				}
			}
			
			if("S".equals(bidDetail.getBidType())) {
				if(bidDetail.getPrice()<=marketCleared.getPrice() && bidDetail.getQuantity()<=marketCleared.getQuantity()) {
					bidDetail.setBidStatus("Provisionally Cleared");
				} else {
					bidDetail.setBidStatus("Rejected");
				}
			}
			gexRepo.updateBidStatus(bidDetail.getBidId(),bidDetail.getBidStatus());
		}
		
	}
	
	public void FinalBidClearing(int timeBlock, String date) throws SQLException, ParseException {
		Date formattedDate=new SimpleDateFormat("yyyy-MM-dd").parse(date);
		List<BidDetails> bidList = gexRepo.getAllBidDetails(date, timeBlock, "Provisionally Cleared", "accepted");
		
		//BidData marketCleared= calculateMCPForGivenSlot(timeBlock,date, "Provisionally Cleared");
		BidData marketCleared =gexRepo.getMCPForGivenSlot(timeBlock,date);
		
		for(BidDetails bidDetail: bidList) {
			if("B".equals(bidDetail.getBidType())) {
				//Check the wallet and clear the bid
			}
			
			if("S".equals(bidDetail.getBidType())) {
				bidDetail.setBidStatus("Cleared");
			}
			gexRepo.updateBidStatus(bidDetail.getBidId(),bidDetail.getBidStatus());
		}
		
	}
	
	public void generateContract(int timeBlock, String date, String bidStatus) throws ParseException, SQLException {
		Date formattedDate=new SimpleDateFormat("yyyy-MM-dd").parse(date);
		//get consolidated bids based on member id
		List<BidDetails> demandBids = gexRepo.getAllBidDetailsForContract(date, timeBlock, bidStatus, "B");
		List<BidDetails> supplyBids = gexRepo.getAllBidDetailsForContract(date, timeBlock, bidStatus, "S");
		
		BidData marketCleared= gexRepo.getMCPForGivenSlot(timeBlock,date);
		/*BidData marketCleared = new BidData();
		marketCleared.setPrice(3.0466666666666673);
		marketCleared.setQuantity(761);*/
		
		List<Contract> contractsList = new ArrayList<>();
		int i=0;
		int j=0;
		
		while(i<demandBids.size() && j<supplyBids.size()) {
			if(demandBids.get(i).getQuantity()<=supplyBids.get(j).getQuantity()) {
				Contract contr=new Contract();
				contr.setBuyer(demandBids.get(i).getMemberId());
				contr.setSeller(supplyBids.get(j).getMemberId());
				double price = marketCleared.getPrice()*1000*demandBids.get(i).getQuantity();
				price=(double)Math.ceil(price);
				gexRepo.updateWalletForContract(demandBids.get(i).getMemberId(),price);
				contr.setPrice(price);
				contr.setQuantity(demandBids.get(i).getQuantity());
				contr.setTimeBlock(timeBlock);
				contr.setDate(date);
				contractsList.add(contr);
				
				supplyBids.get(j).setQuantity(supplyBids.get(j).getQuantity()-demandBids.get(i).getQuantity());
				demandBids.get(i).setQuantity(0);
			} else {
				Contract contr=new Contract();
				contr.setBuyer(demandBids.get(i).getMemberId());
				contr.setSeller(supplyBids.get(j).getMemberId());
				double price = marketCleared.getPrice()*1000*supplyBids.get(j).getQuantity();
				price=(double)Math.ceil(price);
				gexRepo.updateWalletForContract(demandBids.get(i).getMemberId(),price);
				contr.setPrice(price);
				contr.setQuantity(supplyBids.get(j).getQuantity());
				contr.setTimeBlock(timeBlock);
				contr.setDate(date);
				contractsList.add(contr);
				
				demandBids.get(i).setQuantity(demandBids.get(i).getQuantity()-supplyBids.get(j).getQuantity());
				supplyBids.get(j).setQuantity(0);
			}
			
			if(supplyBids.get(j).getQuantity()==0) {
				j++;
			}
			
			if(demandBids.get(i).getQuantity()==0) {
				i++;
			}
		}
		gexRepo.storeContractsInDB(contractsList);
		
	}
	
	public List<BidDetails> getAllBidDetailsForNotification(int memberId, String date) throws SQLException, ParseException {
		Date formattedDate=new SimpleDateFormat("yyyy-MM-dd").parse(date);
		List<BidDetails> bidList = gexRepo.getProvisionallyClearedBids(memberId,date);
		Wallet wallet=gexRepo.getWalletDetails(memberId);
		Double tradedAmount=0.0;
		for(BidDetails bid: bidList) {
			if("B".equals(bid.getBidType())) {
				tradedAmount+=bid.getPrice()*1000*bid.getQuantity();
			}
		}
		Iterator<BidDetails> bidItr=bidList.iterator();
		while(bidItr.hasNext()) {
			BidDetails bid=bidItr.next();
			if("B".equals(bid.getBidType())) {
				if(tradedAmount>wallet.getTotalAmount()) {
					bid.setComments("Insufficient fund. Please add required money to wallet to get the bid cleared");
				} else {
					bidItr.remove();
				}
			}
			if("S".equals(bid.getBidType())) {
				bid.setComments("Please confirm if you can produce the promised amount of energy for the selected time slot");
			}
		}
		
		return bidList;
	}
	
	
	public void comfirmSellBid(int bidId, boolean accept) throws SQLException {
		if(accept) {
			gexRepo.comfirmSellBid(bidId);
		} else {
			gexRepo.updateBidStatus(bidId,"Rejected");
		}
	} 
	
	
	
	
	
	
	public void calculateMCPOld() throws SQLException {

		BidData demand1 = new BidData();
		demand1.setPrice(2.0);
		demand1.setQuantity(830);

		BidData demand2 = new BidData();
		demand2.setPrice(2.5);
		demand2.setQuantity(420);

		// double slope=(demand2.getPrice()-demand1.getPrice())/(demand2.getQuantity()-demand1.getQuantity());
		Double dSlope = calculateSlope(demand1, demand2);

		// double constant = demand1.getPrice()-(slope*demand1.getQuantity());
		Double dConstant = calculateConst(demand1, dSlope);

		log.info("Demand function is: P=" + dSlope + "*D" + dConstant);

		BidData supply1 = new BidData();
		supply1.setPrice(1.3);
		supply1.setQuantity(280);

		BidData supply2 = new BidData();
		supply2.setPrice(2.5);
		supply2.setQuantity(500);

		Double sSlope = calculateSlope(supply1, supply2);

		Double sConstant = calculateConst(supply1, sSlope);

		log.info("Supply function is: P=" + sSlope + "*S" + sConstant);

		Double MCV = (dConstant - sConstant) / (sSlope - dSlope);

		log.info("MCV is: " + MCV);

		Double MCP = (dSlope * MCV) + dConstant;

		log.info("MCP is: " + MCP);
	}
	
	


	private Double calculateSlope(BidData data1, BidData data2) {
		return (data2.getPrice() - data1.getPrice()) / (data2.getQuantity() - data1.getQuantity());
	}

	private Double calculateConst(BidData data, Double slope) {
		return data.getPrice() - (slope * data.getQuantity());
	}
	
	public BidData matching(List<BidData> demandData,List<BidData> supplyData) {
		BidData res= new BidData();
		for(int i=1;i<demandData.size();i++) {
			for(int j=1;j<supplyData.size();j++) {
				if((demandData.get(i-1).getPrice()>=supplyData.get(j-1).getPrice() && demandData.get(i-1).getPrice()<=supplyData.get(j).getPrice())
						|| (demandData.get(i).getPrice()>=supplyData.get(j-1).getPrice() && demandData.get(i).getPrice()<=supplyData.get(j).getPrice())) {
					if((demandData.get(i-1).getQuantity()>=supplyData.get(j-1).getQuantity() && demandData.get(i-1).getQuantity()<=supplyData.get(j).getQuantity())
						|| (demandData.get(i).getQuantity()>=supplyData.get(j-1).getQuantity() && demandData.get(i).getQuantity()<=supplyData.get(j).getQuantity())) {
						Double dSlope = calculateSlope(demandData.get(i-1), demandData.get(i));

						Double dConstant = calculateConst(demandData.get(i-1), dSlope);
						
						Double sSlope = calculateSlope(supplyData.get(j-1), supplyData.get(j));

						Double sConstant = calculateConst(supplyData.get(j-1), sSlope);
						
						Double MCV = (dConstant - sConstant) / (sSlope - dSlope);

						Double MCP = (dSlope * MCV) + dConstant;
						
						res.setPrice(MCP);
						res.setQuantity((int)Math.ceil(MCV));
						
						log.info("MCV is: " + res.getPrice());
						log.info("MCP is: " + res.getQuantity());
						
						boolean result=validateMCP(demandData.get(i-1),demandData.get(i),supplyData.get(j-1),supplyData.get(j),MCV,MCP);
						if(result) {
							return res;
						}
					}
				}
			}
		}
		return res;
	}
	
	
	
	
	private boolean validateMCP(BidData demand1,BidData demand2,BidData supply1,BidData supply2, double MCV, double MCP) {
		if(MCP>= demand1.getPrice() && MCP<=demand2.getPrice() && MCP>= supply1.getPrice() && MCP<=supply2.getPrice()) {
			return true;
		}
		return false;
	}

	public SaveRegistrationRequestStatus login(String userName, String password) {
		SaveRegistrationRequestStatus res = new SaveRegistrationRequestStatus();
		try {
			Member member = gexRepo.getMemberDetailsForLogin(userName);
			if (member == null) {
				res.setMsg("Failed");
				res.setError("Username is invalid");

			} else {
				String decryptPassword = utility.decryptString(member.getPassword());
				if (decryptPassword.equals(password)) {
					res.setMsg("Success");
					res.setRefId(member.getMemberId());
				} else {
					res.setMsg("Failed");
					res.setError("Password is invalid");
				}
			}
		} catch (SQLException e) {
			log.error("Error while validating user login credentials: " + e.getMessage());
			res.setMsg(e.getMessage());
			res.setError("Database Error");
		}
		return res;
	}
	
	public void saveBidInfo(BidInfoDto bidInfo) throws SQLException
	{
		gexRepo.saveBidInfo(bidInfo);
		this.saveBidInfoBlockChain(bidInfo);
	}
	
	public void saveBidInfoBlockChain(BidInfoDto bidData)
	{
		HttpHeaders headers = new HttpHeaders();
		RestTemplate restTemplate = new RestTemplate();
		String serviceurl = "http://54.91.143.236:3000/api/PlaceBid";
		UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(serviceurl);
		List<BidInfoBlockChain> bidDetails = new ArrayList<>();
		try {
			
			Map<Integer, List<BidData>> bidMap = bidData.getBidInfo();
			for(Map.Entry<Integer, List<BidData>> entry: bidMap.entrySet())
			{
				for(int i=0;i<entry.getValue().size();i++)
				{
					BidInfoBlockChain bidBlock = new BidInfoBlockChain();
					BidData bidInfo = entry.getValue().get(i);
					
					bidBlock.setBidId(utility.generateRandomPassword(4));
					bidBlock.setMemberId(String.valueOf(bidData.getMemberId())); //member_id
					bidBlock.setTimeSlot(entry.getKey()); //time_block
					String bidType="";
					if("sell".equalsIgnoreCase(bidInfo.getAction()))
					{
						bidType = "S";
					}	
					else if("buy".equalsIgnoreCase(bidInfo.getAction()))
					{
						bidType = "B";			
					}
					bidBlock.setBidType(bidType);
					bidBlock.setQuantity(bidInfo.getQuantity()); //quantity
					bidBlock.setPrice(bidInfo.getPrice()); //price
					bidBlock.setBidStatus("Placed");
					bidDetails.add(bidBlock); //add the block to the chain

				}
			}
			headers.setContentType(MediaType.APPLICATION_JSON);
			PlaceBidTransaction placeBidTxtn = new PlaceBidTransaction();
			placeBidTxtn.setBids(bidDetails);
			HttpEntity<PlaceBidTransaction> entity = new HttpEntity<>(placeBidTxtn,headers);
			ResponseEntity<PlaceBidTransaction> responseObject = restTemplate.exchange(builder.build().toUri(),
					HttpMethod.POST, entity, PlaceBidTransaction.class);
			
			/*BidInfoBlockChain bidInfo1 = new BidInfoBlockChain();
			bidInfo1.setBidId("534");
			bidInfo1.setBidType("B");
			bidInfo1.setMember("member");
			bidInfo1.setPrice(2.0);
			bidInfo1.setQuantity(30);
			bidInfo1.setTimeSlot(2);
			bidInfo1.setBidStatus("Placed");
			headers.setContentType(MediaType.APPLICATION_JSON);
			HttpEntity<BidInfoBlockChain> entity = new HttpEntity<>(bidInfo1,headers);
			ResponseEntity<BidInfoBlockChain> responseObject = restTemplate.exchange(builder.build().toUri(),
					HttpMethod.POST, entity, BidInfoBlockChain.class);*/
				
			String statusCode = String.valueOf(responseObject.getStatusCodeValue());
			System.out.println("HTTP Status code :"+statusCode);

		} catch (Exception e) {
			log.info("Bid data is not saved in blockchain. HTTP call failed " + e.getMessage());		
		}

	}
	
	public SaveRegistrationRequestStatus rechargeAccount(int memberId, double amount, String action) {
		SaveRegistrationRequestStatus response=new SaveRegistrationRequestStatus();
		try {
			Member member = gexRepo.getMemberDetails(memberId);
			if (member == null) {
				response.setMsg("Failed");
				response.setError("Invalid member Id");
	
			} else {
				if("credit".equalsIgnoreCase(action)) {
					int res = gexRepo.updateWallet(member.getOrg_id(), amount);
					if(res==1) {
						int trans_id = gexRepo.addWalletTransaction(member,amount,"Credit","Online top-up","Success");
						response.setMsg("Success");
						response.setRefId(trans_id);
					}
				} else if("debit".equalsIgnoreCase(action)) {
					int res = gexRepo.updateWallet(member.getOrg_id(), -amount);
					if(res==1) {
						int trans_id = gexRepo.addWalletTransaction(member,amount,"Debit","Online widhdraw","Success");
						response.setMsg("Success");
						response.setRefId(trans_id);
					}
				}
			}
		} catch (SQLException e) {
			log.error("Error while recharging member wallet: " + e.getMessage());
			response.setMsg(e.getMessage());
			response.setError("Database Error");
		}
		return response;
	}
	
	public Wallet getWalletTransactions(int memberId) throws SQLException {
		Wallet wallet=gexRepo.getWalletDetails(memberId);
		List<WalletTransaction> transHistory= gexRepo.getWalletTransactions(memberId);
		wallet.setTransactions(transHistory);
		return wallet;
	}
	
	public List<Contract> getContractDetails(int memberId) throws SQLException {
		return gexRepo.getContractDetails(memberId);	
	}
	
}
