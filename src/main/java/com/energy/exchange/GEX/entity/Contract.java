package com.energy.exchange.GEX.entity;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Contract {
	int id;
	int buyer;
	int seller;
	double price;
	int quantity;
	int timeBlock;
	String date;
	String buyerOrg;
	String sellerOrg;
}
