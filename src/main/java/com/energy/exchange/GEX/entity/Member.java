package com.energy.exchange.GEX.entity;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Member {
	int memberId;
	String emailId;
	String password;
	int org_id;
}
