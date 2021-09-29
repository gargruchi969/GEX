package com.energy.exchange.GEX.util;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.commons.lang3.RandomStringUtils;
import org.jasypt.util.text.BasicTextEncryptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class Utility {

	@Value("${encrypt.password}")
	private String encryptionPassword;

	public static void closeConnection(Connection conn, PreparedStatement pstmt, ResultSet rs) throws SQLException {
		if (conn != null)
			conn.close();
		if (pstmt != null)
			pstmt.close();
		if (rs != null)
			rs.close();
	}

	public String generateRandomPassword(int len) {
		return RandomStringUtils.randomAlphanumeric(len);
	}

	public String encryptString(String input) {
		BasicTextEncryptor textEncryptor = new BasicTextEncryptor();
		textEncryptor.setPasswordCharArray(encryptionPassword.toCharArray());
		return textEncryptor.encrypt(input);
	}

	public String decryptString(String input) {
		BasicTextEncryptor textEncryptor = new BasicTextEncryptor();
		textEncryptor.setPasswordCharArray(encryptionPassword.toCharArray());
		return textEncryptor.decrypt(input);
	}
}
