package com.energy.exchange.GEX.rest;

import java.lang.reflect.Method;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.energy.exchange.GEX.entity.ApproveRegistrationResponse;
import com.energy.exchange.GEX.entity.BidData;
import com.energy.exchange.GEX.entity.BidDetails;
import com.energy.exchange.GEX.entity.BidInfoDto;
import com.energy.exchange.GEX.entity.Contract;
import com.energy.exchange.GEX.entity.RegistrationRequest;
import com.energy.exchange.GEX.entity.SaveRegistrationRequestStatus;
import com.energy.exchange.GEX.entity.Wallet;
import com.energy.exchange.GEX.service.GexUserService;

@RestController
public class GEXService {

	@Value("${DEPLOYEDVERSION}")
	private String deployedVersion;

	@Autowired
	private GexUserService gexUsrSvc;

	@GetMapping("/ping")
	public String ping() {
		StringBuilder pingResponse = new StringBuilder();
		pingResponse.append("Deployed Version " + deployedVersion + "\n\n");
		Method[] methods = GEXService.class.getDeclaredMethods();
		int i = 0;
		pingResponse.append("Deployed Services list:\n");
		while (i < methods.length) {
			pingResponse
					.append((i + 1) + ". " + methods[i].getReturnType().getName() + " " + methods[i].getName() + "\n");
			i++;
		}
		return pingResponse.toString();
	}

	@GetMapping("/getRegistrationReq")
	public List<RegistrationRequest> getRegistrationRequests() throws SQLException {
		return gexUsrSvc.getRegistrationRequests();
	}

	@PostMapping("/saveRegistrationReq")
	public SaveRegistrationRequestStatus getRegistrationRequests(@RequestBody RegistrationRequest request)
			throws SQLException {
		return gexUsrSvc.saveRegistrationRequests(request);
	}

	@PostMapping("/approveRegistration")
	public ApproveRegistrationResponse approveRegistration(@RequestParam Integer refId, @RequestParam String action) {
		return gexUsrSvc.approveRegistrationRequests(refId, action);
	}

	@GetMapping("/calculateMCP")
	public BidData calculateMCP(@RequestParam String date) throws SQLException, ParseException {
		return gexUsrSvc.calculateMCP(date);
	}
	
	@GetMapping("/calculateMCPForGivenSlot")
	public BidData calculateMCPForGivenSlot(@RequestParam int timeSlot, @RequestParam String date) throws SQLException, ParseException {
		return gexUsrSvc.calculateMCPForGivenSlot(timeSlot,date,"Pending");
	}

	@GetMapping("/login")
	public SaveRegistrationRequestStatus userLogin(@RequestParam String userName, @RequestParam String password) {
		return gexUsrSvc.login(userName, password);
	}
	
	@PostMapping("/saveBidInfo")
	public void saveBidInfo(@RequestBody BidInfoDto bidInfo) throws SQLException
	{
		gexUsrSvc.saveBidInfo(bidInfo);
	}
	
	/*@GetMapping("/saveBidInfo")
	public void saveBidInfoBlockChain() throws SQLException
	{
		gexUsrSvc.saveBidInfoBlockChain();
	}*/
	
	@PostMapping("/rechargeAccount")
	public SaveRegistrationRequestStatus rechargeAccount(@RequestParam int memberId, @RequestParam double amount, @RequestParam String action)
	{
		SaveRegistrationRequestStatus response=null;
		if(amount<=0) {
			response=new SaveRegistrationRequestStatus();
			response.setMsg("Failed");
			response.setError("Please provide valid amount");
		} else {
			response = gexUsrSvc.rechargeAccount(memberId,amount,action);
		}
		return response;
	}
	
	@GetMapping("/getWalletHistory")
	public Wallet getWalletTransactions(@RequestParam int memberId) throws SQLException {
		return gexUsrSvc.getWalletTransactions(memberId);
	}
	
	@GetMapping("/getAllBidDetails")
	public List<BidDetails> getAllBidDetailsForNotification(@RequestParam int memberId, @RequestParam String date) throws SQLException, ParseException {
		return gexUsrSvc.getAllBidDetailsForNotification(memberId,date);
	}
	
	@PostMapping("/confirmSellBid")
	public void confirmSellBid(@RequestParam int bidId,@RequestParam boolean accept) throws SQLException
	{
		gexUsrSvc.comfirmSellBid(bidId,accept);
	}
	
	@GetMapping("/calculateMCPOld")
	public void calculateMCPOld() throws SQLException {
		gexUsrSvc.calculateMCPOld();
	}
	
	@GetMapping("/getContractDetails")
	public List<Contract> getContractDetails(@RequestParam int memberId) throws SQLException {
		return gexUsrSvc.getContractDetails(memberId);
	}
	
	@PostMapping("/generateContract")
	public void provisionalClearing(@RequestParam int timeBlock,@RequestParam String date) throws SQLException, ParseException {
		gexUsrSvc.generateContract(timeBlock,date,"Provisionally Cleared");
	}

}
