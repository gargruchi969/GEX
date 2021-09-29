package com.energy.exchange.GEX.entity;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BidDetails {
	int bidId;
	int memberId;
	int timeSlot;
	String bidType;
	int Quantity;
	Double price;
	String bidStatus;
	String comments;
}
