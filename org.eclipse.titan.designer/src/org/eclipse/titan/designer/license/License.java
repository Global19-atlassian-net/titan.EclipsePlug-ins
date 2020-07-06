/******************************************************************************
 * Copyright (c) 2000-2020 Ericsson Telecom AB
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/org/documents/epl-2.0/EPL-2.0.html
 ******************************************************************************/
package org.eclipse.titan.designer.license;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.Signature;
import java.security.SignatureException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import org.eclipse.titan.common.logging.ErrorReporter;
import org.eclipse.titan.common.product.ProductConstants;

/**
 * @author Peter Dimitrov
 * */
public final class License {
	private static final String LICENSE_HEADER = "BEGIN TTCN-3 LICENSE FILE";
	private static final String LICENSE_FOOTER = "END TTCN-3 LICENSE FILE";

	private static final int LIMIT_HOST = 0x01;
	private static final int LIMIT_USER = 0x02;

	private File licenseFile;
	private byte[] rawData;

	private int uniqueID;
	private String licenseeName;
	private String licenseeEmail;
	private String licenseeCompany;
	private String licenseeDepartment;
	private Date validFrom;
	private Date validUntil;
	private int hostID;
	private String loginName;
	private int[] versionFrom = new int[3];
	private int[] versionTo = new int[3];
	private List<String> languageList = new ArrayList<String>();
	private List<String> encoderList = new ArrayList<String>();
	private final List<String> applicationList = new ArrayList<String>();
	private String limitationType = "N/A";
	private int maxPTCs;
	private boolean isValid = false;

	private enum Feature {
		TTCN3("TTCN3", 0x01),
		CODEGEN("CODEGEN", 0x2),
		TPGEN("TPGEN", 0x4),
		SINGLE("SINGLE", 0x8),
		MCTR("MCTR", 0x10),
		HC("HC", 0x20),
		LOGFORMAT("LOGFORMAT", 0x40),
		ASN1("ASN1", 0x80),
		RAW("RAW", 0x100),
		BER("BER", 0x200),
		PER("PER", 0x400),
		GUI("GUI", 0x800),
		TEXT("TEXT", 0x1000),
		XER("XER", 0x2000);

		private final int value;
		private final String text;

		Feature(final String text, final int value) {
			this.text = text;
			this.value = value;
		}

		public String text() {
			return text;
		}

		public int value() {
			return value;
		}
	}

	public License(final String licenseFile) {
		this.licenseFile = new File(licenseFile);
	}

	public void process() {

		FileReader fr = null;
		BufferedReader br = null;
		byte[] array;
		try {
			fr = new FileReader(licenseFile);
			br = new BufferedReader(fr);

			String tempLine = br.readLine();
			while (tempLine != null && !tempLine.contains(LICENSE_HEADER)) {
				tempLine = br.readLine();
			}

			final StringBuilder licenseContents = new StringBuilder(400);
			while (tempLine != null && !tempLine.contains(LICENSE_FOOTER)) {
				tempLine = br.readLine();
				licenseContents.append(tempLine);
			}

			array = licenseContents.toString().getBytes();
		} catch (IOException e) {
			ErrorReporter.logExceptionStackTrace(e);
			return;
		} finally {
			try {
				if (br != null) {
					br.close();
				}
			} catch (IOException e) {
				ErrorReporter.logExceptionStackTrace(e);
			}
		}

		// no license in the file
		if (array.length == 0) {
			return;
		}

		rawData = Base64Decoder.decode(array);

		isValid = checkLicense();
		if (!isValid) {
			return;
		}

		final byte[] tempUniqueID = new byte[4];
		System.arraycopy(rawData, 0, tempUniqueID, 0, 4);
		uniqueID = createInt(tempUniqueID);

		final byte[] tempLicenseeName = new byte[48];
		System.arraycopy(rawData, 4, tempLicenseeName, 0, 48);
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < 48; i++) {
			final char ch = (char) unsignedByteToInt(tempLicenseeName[i]);
			if (ch != 0) {
				sb.append(ch);
			}
		}
		licenseeName = sb.toString();

		final byte[] tempLicenseeEmail = new byte[48];
		System.arraycopy(rawData, 52, tempLicenseeEmail, 0, 48);
		sb = new StringBuilder();
		for (int i = 0; i < 48; i++) {
			final char ch = (char) unsignedByteToInt(tempLicenseeEmail[i]);
			if (ch != 0) {
				sb.append(ch);
			}
		}
		licenseeEmail = sb.toString();

		final byte[] tempLicenseeCompany = new byte[48];
		System.arraycopy(rawData, 100, tempLicenseeCompany, 0, 48);
		sb = new StringBuilder();
		for (int i = 0; i < 48; i++) {
			final char ch = (char) unsignedByteToInt(tempLicenseeCompany[i]);
			if (ch != 0) {
				sb.append(ch);
			}
		}
		licenseeCompany = sb.toString();

		final byte[] tempLicenseeDepartment = new byte[16];
		System.arraycopy(rawData, 148, tempLicenseeDepartment, 0, 16);
		sb = new StringBuilder();
		for (int i = 0; i < 16; i++) {
			final char ch = (char) unsignedByteToInt(tempLicenseeDepartment[i]);
			if (ch != 0) {
				sb.append(ch);
			}
		}
		licenseeDepartment = sb.toString();

		final byte[] tempValidFrom = new byte[4];
		System.arraycopy(rawData, 164, tempValidFrom, 0, 4);
		validFrom = new Date(createInt(tempValidFrom) * 1000L);

		final byte[] tempValidUntil = new byte[4];
		System.arraycopy(rawData, 168, tempValidUntil, 0, 4);
		validUntil = new Date(createInt(tempValidUntil) * 1000L);

		final byte[] tempHostID = new byte[4];
		System.arraycopy(rawData, 172, tempHostID, 0, 4);
		hostID = createInt(tempHostID);

		final byte[] tempLoginName = new byte[8];
		System.arraycopy(rawData, 176, tempLoginName, 0, 8);
		sb = new StringBuilder();
		for (int i = 0; i < 8; i++) {
			final char ch = (char) unsignedByteToInt(tempLoginName[i]);
			if (ch != 0) {
				sb.append(ch);
			}
		}
		loginName = sb.toString();

		final byte[] from = new byte[4];
		System.arraycopy(rawData, 184, from, 0, 4);
		this.versionFrom[0] = createInt(from);
		System.arraycopy(rawData, 188, from, 0, 4);
		this.versionFrom[1] = createInt(from);
		System.arraycopy(rawData, 192, from, 0, 4);
		this.versionFrom[2] = createInt(from);

		final byte[] to = new byte[4];
		System.arraycopy(rawData, 196, to, 0, 4);
		this.versionTo[0] = createInt(to);
		System.arraycopy(rawData, 200, to, 0, 4);
		this.versionTo[1] = createInt(to);
		System.arraycopy(rawData, 204, to, 0, 4);
		this.versionTo[2] = createInt(to);

		final byte[] temp = new byte[4];
		System.arraycopy(rawData, 208, temp, 0, 4);
		final int features = createInt(temp);
		if ((features & Feature.TTCN3.value()) == Feature.TTCN3.value()) {
			languageList.add(Feature.TTCN3.text());
		}
		if ((features & Feature.ASN1.value()) == Feature.ASN1.value()) {
			languageList.add(Feature.ASN1.text());
		}

		if ((features & Feature.RAW.value()) == Feature.RAW.value()) {
			encoderList.add(Feature.RAW.text());
		}
		if ((features & Feature.TEXT.value()) == Feature.TEXT.value()) {
			encoderList.add(Feature.TEXT.text());
		}
		if ((features & Feature.BER.value()) == Feature.BER.value()) {
			encoderList.add(Feature.BER.text());
		}
		if ((features & Feature.PER.value()) == Feature.PER.value()) {
			encoderList.add(Feature.PER.text());
		}
		if ((features & Feature.XER.value()) == Feature.XER.value()) {
			encoderList.add(Feature.XER.text());
		}

		if ((features & Feature.CODEGEN.value()) == Feature.CODEGEN.value()) {
			applicationList.add(Feature.CODEGEN.text());
		}
		if ((features & Feature.TPGEN.value()) == Feature.TPGEN.value()) {
			applicationList.add(Feature.TPGEN.text());
		}
		if ((features & Feature.SINGLE.value()) == Feature.SINGLE.value()) {
			applicationList.add(Feature.SINGLE.text());
		}
		if ((features & Feature.MCTR.value()) == Feature.MCTR.value()) {
			applicationList.add(Feature.MCTR.text());
		}
		if ((features & Feature.HC.value()) == Feature.HC.value()) {
			applicationList.add(Feature.HC.text());
		}
		if ((features & Feature.LOGFORMAT.value()) == Feature.LOGFORMAT.value()) {
			applicationList.add(Feature.LOGFORMAT.text());
		}
		if ((features & Feature.GUI.value()) == Feature.GUI.value()) {
			applicationList.add(Feature.GUI.text());
		}

		System.arraycopy(rawData, 212, temp, 0, 4);
		final int limitations = createInt(temp);
		if ((limitations & LIMIT_HOST) == LIMIT_HOST) {
			limitationType = "HOST";
		} else if ((limitations & LIMIT_USER) == LIMIT_USER) {
			limitationType = "USER";
		}

		System.arraycopy(rawData, 216, temp, 0, 4);
		maxPTCs = createInt(temp);

	}

	private boolean checkLicense() {
		final byte[] message = new byte[220];
		System.arraycopy(rawData, 0, message, 0, 220);
		final byte[] dsaSignature = new byte[48];
		System.arraycopy(rawData, 220, dsaSignature, 0, 48);
		boolean result = false;
		Signature dsa = null;
		try {
			dsa = Signature.getInstance("DSA");
			dsa.initVerify(new TITANDSAPublicKey());
		} catch (NoSuchAlgorithmException e) {
			ErrorReporter.logExceptionStackTrace("DSA algorithm is not known by Java", e);
			return false;
		} catch (InvalidKeyException e) {
			ErrorReporter.logExceptionStackTrace("TITAN public key is invalid", e);
			return false;
		}

		// find the true length of the dsa signature, without the trailing zeros.
		int length = 48;
		while (length > 0 && dsaSignature[length-1] == 0) {
			length--;
		}
		try {
			dsa.update(message);
			result = dsa.verify(dsaSignature, 0, length);
		} catch (SignatureException e) {
			ErrorReporter.logExceptionStackTrace(e);
		}

		return result;
	}

	public boolean isValid() {
		return isValid;
	}

	public int getUniqueID() {
		return uniqueID;
	}

	public String getLicenseeName() {
		return licenseeName;
	}

	public String getLicenseeEmail() {
		return licenseeEmail;
	}

	public String getLicenseeCompany() {
		return licenseeCompany;
	}

	public String getLicenseeDepartment() {
		return licenseeDepartment;
	}

	public Date getValidFrom() {
		return (Date) validFrom.clone();
	}

	public Date getValidUntil() {
		return (Date) validUntil.clone();
	}

	public int getHostID() {
		return hostID;
	}

	public String getLoginName() {
		return loginName;
	}

	public int[] getVersionFrom() {
		return versionFrom.clone();
	}

	public int[] getVersionTo() {
		return versionTo.clone();
	}

	public List<String> getLanguageList() {
		return languageList;
	}

	public List<String> getEncoderList() {
		return encoderList;
	}

	public List<String> getApplicationList() {
		return applicationList;
	}

	public String getLimitationType() {
		return limitationType;
	}

	public int getMaxPTCs() {
		return maxPTCs;
	}

	public int unsignedByteToInt(final byte b) {
		return b & 0xFF;
	}

	public int createInt(final byte[] number) {
		if (number.length != 4) {
			return -1;
		}

		return (unsignedByteToInt(number[0]) << 24) + (unsignedByteToInt(number[1]) << 16) + (unsignedByteToInt(number[2]) << 8)
				+ unsignedByteToInt(number[3]);
	}

	@Override
	public String toString() {
		final StringBuilder builder = new StringBuilder(300);
		builder.append("Unique ID\t: ").append(getUniqueID()).append('\n');
		builder.append("Licensee\t: ").append(getLicenseeName()).append('\n');
		builder.append("E-mail\t\t: ").append(getLicenseeEmail()).append('\n');
		builder.append("Company\t: ").append(getLicenseeCompany()).append('\n');
		builder.append("Department\t: ").append(getLicenseeDepartment()).append('\n');

		final SimpleDateFormat dFormat = new SimpleDateFormat("yyyy-MM-dd E HH:mm:ss z");
		dFormat.setTimeZone(TimeZone.getTimeZone("GMT+2"));
		builder.append("Valid from\t: ").append(dFormat.format(getValidFrom())).append('\n');
		builder.append("Valid until\t: ").append(dFormat.format(getValidUntil())).append('\n');
		builder.append("Limitation\t: ").append(getLimitationType()).append('\n');
		builder.append("Host ID\t: ").append(getHostID()).append('\n');
		builder.append("Login name\t: ").append(getLoginName()).append('\n');
		final int[] from = getVersionFrom();
		final int[] until = getVersionTo();

		builder.append("Versions\t: from ").append(from[0]).append('.').append(from[1]).append(".pl").append(from[2]);
		builder.append(" until ").append(until[0]).append('.').append(until[1]).append(".pl").append(until[2]).append('\n');
		builder.append("Languages\t:");
		for (final String temp : getLanguageList()) {
			builder.append(' ').append(temp);
		}
		builder.append('\n');
		builder.append("Encoders\t:");
		for (final String temp : getEncoderList()) {
			builder.append(' ').append(temp);
		}
		builder.append('\n');
		builder.append("Applications\t:");
		for (final String temp : getApplicationList()) {
			builder.append(' ').append(temp);
		}
		builder.append('\n');
		builder.append("Max PTCs\t: ").append(getMaxPTCs()).append('\n');

		return builder.toString();
	}

	/**
	 * @return if usage statistics is sent
	 */
	public static boolean isLicenseNeeded() {
		return ProductConstants.LICENSE_NEEDED;
	}
}
