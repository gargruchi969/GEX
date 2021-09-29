package com.energy.exchange.GEX.entity;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Wallet {
	int wallet_id;
	double totalAmount;
	double tradedAmount;
	List<WalletTransaction> transactions;
}
