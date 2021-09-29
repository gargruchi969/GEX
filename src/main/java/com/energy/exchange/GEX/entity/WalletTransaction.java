package com.energy.exchange.GEX.entity;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class WalletTransaction {
	private int id;
	private String transaction;
	private double amount;
	private String date;
	private String transMode;
	private String status;
}
