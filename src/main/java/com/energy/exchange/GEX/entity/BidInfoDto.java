package com.energy.exchange.GEX.entity;

import java.util.List;
import java.util.Map;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BidInfoDto {
	int memberId;
	Map<Integer, List<BidData>> bidInfo;
}
