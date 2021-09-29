package com.energy.exchange.GEX.entity;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BidInfoBlockChain {
	
	String bidId;
	String bidType;
	int quantity;
	Double price;
	int timeSlot;
	String memberId;
	String bidStatus;


}
