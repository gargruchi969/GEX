package com.energy.exchange.GEX.entity;

/*import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;*/

import lombok.Getter;
import lombok.Setter;

/*@Entity
@Table(name="member_registration_request")
public class RegistrationRequest {
	
	@Column(name="reg_req_id")
	private int id;
	
	@Column(name="email_id")
	private String emailId;
	
	@Column(name="org_name")
	private String orgName;
	
	@Column(name="name")
	private String name;
	
	@Column(name = "location")
	private String location;
	
	@Column(name="address")
	private String address;
	
	@Column(name = "contact_number")
	private String mobileNumber;
	
	@Column(name="designation")
	private String designation;
	
	@Column(name="status")
	private String status;
	


}*/
@Getter
@Setter
public class RegistrationRequest {
	
	
	private int id;
	private String emailId;
	private String orgName;
	private String name;
	private String location;
	private String address;
	private String mobileNumber;
	private String designation;
	private String status;
	
}
