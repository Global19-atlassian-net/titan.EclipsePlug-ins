/******************************************************************************
 * Copyright (c) 2000-2018 Ericsson Telecom AB
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/org/documents/epl-2.0/EPL-2.0.html
 ******************************************************************************/
package org.eclipse.titan.executor.tabpages.testset;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import org.eclipse.swt.dnd.ByteArrayTransfer;
import org.eclipse.swt.dnd.TransferData;
import org.eclipse.titan.common.logging.ErrorReporter;

/**
 * @author Kristof Szabados
 * */
public final class TestcaseTransfer extends ByteArrayTransfer {
	private static final TestcaseTransfer INSTANCE = new TestcaseTransfer();
	private static final String TYPE_NAME = "TITAN-testcase-transfer-format";
	private static final int TYPEID = registerType(TYPE_NAME);

	private TestcaseTransfer() {
	}

	public static TestcaseTransfer getInstance() {
		return INSTANCE;
	}

	protected TestCaseTreeElement[] fromByteArray(final byte[] bytes) {
		final DataInputStream in = new DataInputStream(new ByteArrayInputStream(bytes));
		TestCaseTreeElement[] testcases = null;

		try {
			final int size = in.readInt();
			testcases = new TestCaseTreeElement[size];
			for (int i = 0; i < size; i++) {
				final TestCaseTreeElement testcase = new TestCaseTreeElement(in.readUTF());
				testcases[i] = testcase;
			}
		} catch (IOException e) {
			ErrorReporter.logExceptionStackTrace(e);
		}

		try {
			in.close();
		} catch (IOException e) {
			ErrorReporter.logExceptionStackTrace(e);
		}

		return testcases;
	}

	@Override
	protected int[] getTypeIds() {
		return new int[] {TYPEID};
	}

	@Override
	protected String[] getTypeNames() {
		return new String[] {TYPE_NAME};
	}

	@Override
	protected void javaToNative(final Object object, final TransferData transferData) {
		final byte[] bytes = toByteArray((TestCaseTreeElement[]) object);
		if (null != bytes) {
			super.javaToNative(bytes, transferData);
		}
	}

	@Override
	protected TestCaseTreeElement[] nativeToJava(final TransferData transferData) {
		final byte[] bytes = (byte[]) super.nativeToJava(transferData);
		return fromByteArray(bytes);
	}

	protected byte[] toByteArray(final TestCaseTreeElement[] testcases) {
		final ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
		final DataOutputStream out = new DataOutputStream(byteOut);

		byte[] bytes = null;

		try {
			out.writeInt(testcases.length);
			for (final TestCaseTreeElement testcase : testcases) {
				out.writeUTF((testcase).name());
			}

			bytes = byteOut.toByteArray();
		} catch (IOException e) {
			ErrorReporter.logExceptionStackTrace(e);
		} finally {
			try {
				out.close();
				byteOut.close();
			} catch (IOException e) {
				ErrorReporter.logExceptionStackTrace(e);
			}
		}

		return bytes;
	}
}
