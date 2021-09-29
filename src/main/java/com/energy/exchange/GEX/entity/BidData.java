package com.energy.exchange.GEX.entity;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BidData {
	private Double price;
	private int quantity;
	private String action;
}
