package com.energy.exchange.GEX.entity;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ApproveRegistrationResponse {
	private String msg;
	private String userName;
	private String password;
	private String error;
}
