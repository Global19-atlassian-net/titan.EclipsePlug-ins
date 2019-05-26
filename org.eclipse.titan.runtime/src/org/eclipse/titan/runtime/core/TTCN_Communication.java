/******************************************************************************
 * Copyright (c) 2000-2019 Ericsson Telecom AB
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/org/documents/epl-2.0/EPL-2.0.html
 ******************************************************************************/
package org.eclipse.titan.runtime.core;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.channels.NetworkChannel;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.AbstractSelectableChannel;
import java.text.MessageFormat;
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.titan.runtime.core.Event_Handler.Channel_And_Timeout_Event_Handler;
import org.eclipse.titan.runtime.core.NetworkHandler.HCNetworkHandler;
import org.eclipse.titan.runtime.core.NetworkHandler.NetworkFamily;
import org.eclipse.titan.runtime.core.TTCN_Logger.Severity;
import org.eclipse.titan.runtime.core.TTCN_Runtime.executorStateEnum;
import org.eclipse.titan.runtime.core.TitanLoggerApi.ExecutorConfigdata_reason;
import org.eclipse.titan.runtime.core.TitanLoggerApi.ExecutorUnqualified_reason.enum_type;
import org.eclipse.titan.runtime.core.TitanPort.Map_Params;
import org.eclipse.titan.runtime.core.TitanVerdictType.VerdictTypeEnum;
import org.eclipse.titan.runtime.core.cfgparser.CfgAnalyzer;

/**
 * The class handling internal communication.
 *
 * TODO: lots to implement
 *
 * @author Kristof Szabados
 */
public final class TTCN_Communication {
	/* Any relation - any direction */

	private static final int MSG_ERROR = 0;

	/* Any relation - to MC (up) */

	private static final int MSG_LOG = 1;

	/* First messages - to MC (up) */

	/* from HCs */
	private static final int MSG_VERSION = 2;
	/* from MTC */
	private static final int MSG_MTC_CREATED = 3;
	/* from PTCs */
	private static final int MSG_PTC_CREATED = 4;

	/* Messages from MC to HC (down) */

	private static final int MSG_CREATE_MTC = 2;
	private static final int MSG_CREATE_PTC = 3;
	private static final int MSG_KILL_PROCESS = 4;
	private static final int MSG_EXIT_HC = 5;

	/* Messages from HC to MC (up) */

	private static final int MSG_CREATE_NAK = 4;
	private static final int MSG_HC_READY = 5;

	/* Messages from MC to TC (down) */

	private static final int MSG_CREATE_ACK = 1;
	private static final int MSG_START_ACK = 2;
	private static final int MSG_STOP = 3;
	private static final int MSG_STOP_ACK = 4;
	private static final int MSG_KILL_ACK = 5;
	private static final int MSG_RUNNING = 6;
	private static final int MSG_ALIVE = 7;
	private static final int MSG_DONE_ACK = 8;
	private static final int MSG_KILLED_ACK = 9;
	private static final int MSG_CANCEL_DONE = 10;
	private static final int MSG_COMPONENT_STATUS = 11;
	private static final int MSG_CONNECT_LISTEN = 12;
	private static final int MSG_CONNECT = 13;
	private static final int MSG_CONNECT_ACK = 14;
	private static final int MSG_DISCONNECT = 15;
	private static final int MSG_DISCONNECT_ACK = 16;
	private static final int MSG_MAP = 17;
	private static final int MSG_MAP_ACK = 18;
	private static final int MSG_UNMAP = 19;
	private static final int MSG_UNMAP_ACK = 20;

	/* Messages from MC to MTC (down) */

	private static final int MSG_EXECUTE_CONTROL = 21;
	private static final int MSG_EXECUTE_TESTCASE = 22;
	private static final int MSG_PTC_VERDICT = 23;
	private static final int MSG_CONTINUE = 24;
	private static final int MSG_EXIT_MTC = 25;

	/* Messages from MC to PTC (down) */

	private static final int MSG_START = 21;
	private static final int MSG_KILL = 22;

	/* Messages from TC to MC (up) */

	private static final int MSG_CREATE_REQ = 2;
	private static final int MSG_START_REQ = 3;
	private static final int MSG_STOP_REQ = 4;
	private static final int MSG_KILL_REQ = 5;
	private static final int MSG_IS_RUNNING = 6;
	private static final int MSG_IS_ALIVE = 7;
	private static final int MSG_DONE_REQ = 8;
	private static final int MSG_KILLED_REQ = 9;
	private static final int MSG_CANCEL_DONE_ACK = 10;
	private static final int MSG_CONNECT_REQ = 11;
	private static final int MSG_CONNECT_LISTEN_ACK = 12;
	private static final int MSG_CONNECTED = 13;
	private static final int MSG_CONNECT_ERROR = 14;
	private static final int MSG_DISCONNECT_REQ = 15;
	private static final int MSG_DISCONNECTED = 16;
	private static final int MSG_MAP_REQ = 17;
	private static final int MSG_MAPPED = 18;
	private static final int MSG_UNMAP_REQ = 19;
	private static final int MSG_UNMAPPED = 20;
	private static final int MSG_DEBUG_HALT_REQ = 101;
	private static final int MSG_DEBUG_CONTINUE_REQ = 102;
	private static final int MSG_DEBUG_BATCH = 103;

	/* Messages from MTC to MC (up) */

	private static final int MSG_TESTCASE_STARTED = 21;
	private static final int MSG_TESTCASE_FINISHED = 22;
	private static final int MSG_MTC_READY = 23;

	/* Messages from PTC to MC (up) */

	private static final int MSG_STOPPED = 21;
	private static final int MSG_STOPPED_KILLED = 22;
	private static final int MSG_KILLED = 23;

	/* Messages from MC to HC or TC (down) */

	private static final int MSG_DEBUG_COMMAND = 100;

	/* Messages from HC or TC to MC (up) */

	private static final int MSG_DEBUG_RETURN_VALUE = 100;

	/* Messages from MC to HC or MTC (down) */

	private static final int MSG_CONFIGURE = 200;

	/* Messages from HC or MTC to MC (up) */

	private static final int MSG_CONFIGURE_ACK = 200;
	private static final int MSG_CONFIGURE_NAK = 201;

	public static enum transport_type_enum {
		TRANSPORT_LOCAL,
		TRANSPORT_INET_STREAM,
		/* unsupported as it is not platform independent */
		@Deprecated
		TRANSPORT_UNIX_STREAM,
		/*unused marks the current number of enumerations */
		TRANSPORT_NUM
	}

	private static boolean mc_addr_set = false;
	private static ThreadLocal<Boolean> is_connected = new ThreadLocal<Boolean>() {
		@Override
		protected Boolean initialValue() {
			return false;
		}
	};

	private static String MC_host;
	private static int MC_port;
	private static HCNetworkHandler hcnh = new HCNetworkHandler();
	private static boolean local_addr_set = false;

	private static ThreadLocal<SocketChannel> mc_socketchannel = new ThreadLocal<SocketChannel>() {
		@Override
		protected SocketChannel initialValue() {
			return null;
		}
	};

	private static ThreadLocal<MC_Connection> mc_connection = new ThreadLocal<TTCN_Communication.MC_Connection>() {
		@Override
		protected MC_Connection initialValue() {
			return null;
		}
	};

	private static ThreadLocal<Double> call_interval = new ThreadLocal<Double>() {
		@Override
		protected Double initialValue() {
			return 0.0;
		}
	};

	//private static DataOutputStream mc_outputstream;
	private static ThreadLocal<Text_Buf> incoming_buf = new ThreadLocal<Text_Buf>() {
		@Override
		protected Text_Buf initialValue() {
			return new Text_Buf();
		}
	};

	static class MC_Connection extends Channel_And_Timeout_Event_Handler {
		final SocketChannel mc_channel;
		final Text_Buf incoming_buffer;

		MC_Connection(final SocketChannel channel, final Text_Buf buffer) {
			mc_channel = channel;
			incoming_buffer = buffer;
		}

		@Override
		public void Handle_Event(final SelectableChannel channel, final boolean is_readable, final boolean is_writeable) {
			if (!channel.equals(mc_channel)) {
				throw new TtcnError("MC_Connection: unexpected selectable channel.");
			}
			//FIXME implement
			if (is_readable) {
				final AtomicInteger buf_ptr = new AtomicInteger();
				final AtomicInteger buf_len = new AtomicInteger();
				incoming_buffer.get_end(buf_ptr, buf_len);

				final ByteBuffer tempbuffer = ByteBuffer.allocate(1024);
				int recv_len = 0;
				try {
					recv_len = mc_channel.read(tempbuffer);
				} catch (IOException e) {
					throw new TtcnError(e);
				}
				if (recv_len > 0) {
					//incoming_buf.increase_length(recv_len);
					incoming_buf.get().push_raw(recv_len, tempbuffer.array());

					if (!TTCN_Runtime.is_idle()) {
						process_all_messages_tc();
					}
				} else {
					close_mc_connection();
					if (recv_len == 0) {
						throw new TtcnError("Control connection was closed unexpectedly by MC.");
					} else {
						throw new TtcnError("Receiving data on the control connection from MC failed.");
					}
					//FIXME implement
				}
			}
		}
		//FIXME implement

		@Override
		public void Handle_Timeout(final double time_since_last_call) {
			if (TTCN_Runtime.get_state() == executorStateEnum.HC_OVERLOADED) {
				// indicate the timeout to be handled in process_all_messages_hc()
				TTCN_Runtime.set_state(executorStateEnum.HC_OVERLOADED_TIMEOUT);
			} else {
				TtcnError.TtcnWarning("Unexpected timeout occurred on the control connection to MC.");
				disable_periodic_call();
			}
		}
		
	}
	
	public static NetworkFamily get_network_family() {
		return hcnh.get_family();
	}
	
	public static boolean has_local_address() {
		return local_addr_set;
	}
	
	public static void set_local_address(final String host_name) {
		if (local_addr_set) {
			TtcnError.TtcnWarning("The local address has already been set.");
		}
		if (is_connected.get()) {
			throw new TtcnError("Trying to change the local address, but there is an existing control connection to MC.");
		}
		if (host_name == null) {
			throw new TtcnError("TTCN_Communication.set_local_address: internal error: invalid host name."); // There is no connection to the MC
		}
		if (!hcnh.set_local_addr(host_name, 0)) {
			throw new TtcnError(MessageFormat.format("Could not get the IP address for the local address ({0}): Host name lookup failure.", host_name));
		}
		TTCN_Logger.log_executor_misc(enum_type.local__address__was__set, hcnh.get_local_host_str(), hcnh.get_local_addr_str(), 0);
		local_addr_set = true;
	}
	
	public static InetAddress get_local_address() {
		if (!local_addr_set) {
			throw new TtcnError("TTCN_Communication.get_local_address: internal error: the local address has not been set.");
		}
		return hcnh.get_local_addr().getAddress();
	}

	public static void set_mc_address(final String MC_host, final int MC_port) {
		if (mc_addr_set) {
			TtcnError.TtcnWarning("The address of MC has already been set.");
		}
		if (is_connected.get()) {
			throw new TtcnError("Trying to change the address of MC, but there is an existing connection.");
		}
		if (MC_host == null) {
			throw new TtcnError("TTCN_Communication.set_mc_address: internal error: invalid host name.");
		}
		if (MC_port < 0) {
			throw new TtcnError(MessageFormat.format("TTCN_Communication.set_mc_address: internal error: invalid TCP port. {0,number,#}", MC_port));
		}
		hcnh.set_family(new InetSocketAddress(MC_host, MC_port));
		if (!hcnh.set_mc_addr(MC_host, MC_port)) {
			throw new TtcnError(MessageFormat.format("Could not get the IP address of MC ({0}): Host name lookup failure.", MC_host));
		}
		if (hcnh.is_local(hcnh.get_mc_addr())) {
			TtcnError.TtcnWarning("The address of MC was set to a local IP address. This may cause incorrect behavior if a HC from a remote host also connects to MC.");
		}
		TTCN_Logger.log_executor_misc(enum_type.address__of__mc__was__set, hcnh.get_mc_host_str(), hcnh.get_mc_addr_str(), hcnh.get_mc_port());
		TTCN_Communication.MC_host = MC_host;
		TTCN_Communication.MC_port = MC_port;
		mc_addr_set = true;
	}

	public static boolean is_mc_connected() {
		return is_connected.get();
	}

	public static void connect_mc() {
		if (is_connected.get()) {
			throw new TtcnError("Trying to re-connect to MC, but there is an existing connection.");
		}
		if (!mc_addr_set) {
			throw new TtcnError("Trying to connect to MC, but the address of MC has not yet been set.");
		}
		mc_socketchannel.set(hcnh.connect_to_mc());
		if (mc_socketchannel.get() == null) {
			throw new TtcnError(MessageFormat.format("Connecting to MC failed. MC address: {0}:{1,number,#} \r\n", hcnh.get_mc_addr_str(), hcnh.get_mc_port()));
		}
		//FIXME register
		mc_connection.set(new MC_Connection(mc_socketchannel.get(), incoming_buf.get()));
		try {
			mc_socketchannel.get().configureBlocking(false);
			TTCN_Snapshot.channelMap.get().put(mc_socketchannel.get(), mc_connection.get());
			mc_socketchannel.get().register(TTCN_Snapshot.selector.get(), SelectionKey.OP_READ);
		} catch (IOException e) {
			throw new TtcnError(e);
		}

		TTCN_Logger.log_executor_runtime(TitanLoggerApi.ExecutorRuntime_reason.enum_type.connected__to__mc);
		is_connected.set(true);
	}

	public static void disconnect_mc() {
		if (is_connected.get()) {
			// TODO check if the missing part is needed
			close_mc_connection();
			TTCN_Logger.log_executor_runtime(TitanLoggerApi.ExecutorRuntime_reason.enum_type.disconnected__from__mc);
		}
	}

	public static void close_mc_connection() {
		if (is_connected.get()) {
			call_interval.set(0.0);
			is_connected.set(false);
			incoming_buf.get().reset();
			try {
				mc_socketchannel.get().close();
			} catch (IOException e) {
				throw new TtcnError(e);
			}

			TTCN_Snapshot.channelMap.get().remove(mc_socketchannel);
			TTCN_Snapshot.set_timer(mc_connection.get(), 0.0, true, true, true);
		}
	}

	//use NetworkChannel instead of file descriptor
	public static boolean set_tcp_nodelay(final NetworkChannel fd, final Boolean enable_nodelay) {
		try {
			fd.setOption(StandardSocketOptions.TCP_NODELAY, enable_nodelay);
			return true;
		} catch (IOException e) {
			TTCN_Logger.begin_event(Severity.ERROR_UNQUALIFIED);
			TTCN_Logger.log_event(e.toString());
			TTCN_Logger.end_event();
			return false;
		}
	}

	//use AbstractSelectableChannel instead of file descriptor
	public static boolean set_non_blocking_mode(final AbstractSelectableChannel fd, final boolean enable_nonblock) {
		try {
			fd.configureBlocking(!enable_nonblock);
			return true;
		} catch (IOException e) {
			TTCN_Logger.begin_event(Severity.ERROR_UNQUALIFIED);
			TTCN_Logger.log_event(e.toString());
			TTCN_Logger.end_event();
			return false;
		}
	}

	public static void enable_periodic_call() {
		call_interval.set(1.0);
		TTCN_Snapshot.set_timer(mc_connection.get(), call_interval.get(), true, false, true);
	}

	public static void increase_call_interval() {
		if (call_interval.get() == null || call_interval.get() <= 0.0) {
			throw new TtcnError("INternal error: TTCN_Communication.increase_call_interval was called when call interval is not set.");
		}

		call_interval.set(call_interval.get() * 2.0);
		TTCN_Snapshot.set_timer(mc_connection.get(), call_interval.get(), true, false, true);
	}

	public static void disable_periodic_call() {
		TTCN_Snapshot.set_timer(mc_connection.get(), 0.0, true, true, true);
		call_interval.set(0.0);
	}

	public static void process_all_messages_hc() {
		if (!TTCN_Runtime.is_hc()) {
			throw new TtcnError("Internal error: TTCN_Communication.process_all_messages_hc() was called in invalid state.");
		}

		TTCN_Runtime.wait_terminated_processes();
		boolean wait_flag = false;
		boolean check_overload = TTCN_Runtime.is_overloaded();
		final Text_Buf local_incoming_buf = incoming_buf.get();
		while (local_incoming_buf.is_message()) {
			wait_flag = true;

			final int msg_len = local_incoming_buf.pull_int().get_int();
			final int msg_end = local_incoming_buf.get_pos() + msg_len;
			final int msg_type = local_incoming_buf.pull_int().get_int();

			switch (msg_type) {
			case MSG_ERROR:
				process_error();
				break;
			case MSG_CONFIGURE:
				process_configure(msg_end, false);
				break;
			case MSG_CREATE_MTC:
				process_create_mtc();
				TTCN_Runtime.wait_terminated_processes();
				wait_flag = false;
				check_overload = false;
				break;
			case MSG_CREATE_PTC:
				process_create_ptc();
				TTCN_Runtime.wait_terminated_processes();
				wait_flag = false;
				check_overload = false;
				break;
			case MSG_KILL_PROCESS:
				process_kill_process();
				TTCN_Runtime.wait_terminated_processes();
				wait_flag = false;
				break;
			case MSG_EXIT_HC:
				process_exit_hc();
				break;
			case MSG_DEBUG_COMMAND:
				process_debug_command();
				break;
			default:
				process_unsupported_message(msg_type, msg_end);
				break;
			}
		}
		if (wait_flag) {
			TTCN_Runtime.wait_terminated_processes();
		}
		if (check_overload && TTCN_Runtime.is_overloaded()) {
			//FIXME implement check_overload
		}
	}

	public static void process_all_messages_tc() {
		if (!TTCN_Runtime.is_tc()) {
			throw new TtcnError("Internal error: TTCN_Communication.process_all_messages_tc() was called in invalid state.");
		}

		final Text_Buf local_incoming_buf = incoming_buf.get();
		while (local_incoming_buf.is_message()) {
			final int msg_len = local_incoming_buf.pull_int().get_int();
			final int msg_end = local_incoming_buf.get_pos() + msg_len;
			final int msg_type = local_incoming_buf.pull_int().get_int();

			// messages: MC -> TC
			switch (msg_type) {
			case MSG_ERROR:
				process_error();
				break;
			case MSG_CREATE_ACK:
				process_create_ack();
				break;
			case MSG_START_ACK:
				process_start_ack();
				break;
			case MSG_STOP:
				process_stop();
				break;
			case MSG_STOP_ACK:
				process_stop_ack();
				break;
			case MSG_KILL_ACK:
				process_kill_ack();
				break;
			case MSG_RUNNING:
				process_running();
				break;
			case MSG_ALIVE:
				process_alive();
				break;
			case MSG_DONE_ACK:
				process_done_ack(msg_end);
				break;
			case MSG_KILLED_ACK:
				process_killed_ack();
				break;
			case MSG_CANCEL_DONE:
				if (TTCN_Runtime.is_mtc()) {
					process_cancel_done_mtc();
				} else {
					process_cancel_done_ptc();
				}
				break;
			case MSG_COMPONENT_STATUS:
				if (TTCN_Runtime.is_mtc()) {
					process_component_status_mtc(msg_end);
				} else {
					process_component_status_ptc(msg_end);
				}
				break;
			case MSG_CONNECT_LISTEN:
				process_connect_listen();
				break;
			case MSG_CONNECT:
				process_connect();
				break;
			case MSG_CONNECT_ACK:
				process_connect_ack();
				break;
			case MSG_DISCONNECT:
				process_disconnect();
				break;
			case MSG_DISCONNECT_ACK:
				process_disconnect_ack();
				break;
			case MSG_MAP:
				process_map();
				break;
			case MSG_MAP_ACK:
				process_map_ack();
				break;
			case MSG_UNMAP:
				process_unmap();
				break;
			case MSG_UNMAP_ACK:
				process_unmap_ack();
				break;
			case MSG_DEBUG_COMMAND:
				//FIXME process_debug_command();
				throw new TtcnError("MSG_DEBUG_COMMAND received, but not yet supported!");
			default:
				if (TTCN_Runtime.is_mtc()) {
					// messages: MC -> MTC
					switch(msg_type) {
					case MSG_EXECUTE_CONTROL:
						process_execute_control();
						break;
					case MSG_EXECUTE_TESTCASE:
						process_execute_testcase();
						break;
					case MSG_PTC_VERDICT:
						process_ptc_verdict();
						break;
					case MSG_CONTINUE:
						//FIXME process_continue();
						throw new TtcnError("MSG_CONTINUE received, but not yet supported!");
					case MSG_EXIT_MTC:
						process_exit_mtc();
						break;
					case MSG_CONFIGURE:
						//FIXME process_configure(msg_end, TRUE);
						throw new TtcnError("MSG_CONFIGURE received, but not yet supported!");
					default:
						process_unsupported_message(msg_type, msg_end);
						break;
					}
				} else {
					// messages: MC -> PTC
					switch (msg_type) {
					case MSG_START:
						process_start();
						break;
					case MSG_KILL:
						process_kill();
						break;
					default:
						process_unsupported_message(msg_type, msg_end);
						break;
					}
				}
			}
		}
	}

	public static void send_version() {
		//FIXME implement (only temporary values for now)

		final Text_Buf text_buf = new Text_Buf();
		text_buf.push_int(MSG_VERSION);
		//sending temporary data
		text_buf.push_int(TTCN_Runtime.TTCN3_MAJOR);
		text_buf.push_int(TTCN_Runtime.TTCN3_MINOR);
		text_buf.push_int(TTCN_Runtime.TTCN3_PATCHLEVEL);
		text_buf.push_int(TTCN_Runtime.TTCN3_BUILDNUMBER);

		Module_List.push_version(text_buf);

		//FIXME fill with correct machine info
		text_buf.push_string(TTCN_Runtime.get_host_name());//node
		text_buf.push_string(System.getProperty("os.arch"));//machine
		text_buf.push_string(System.getProperty("os.name"));//sysname
		text_buf.push_string(System.getProperty("os.version"));//release
		text_buf.push_string("FIXME");//version

		text_buf.push_int(2);//nof supported transports
		text_buf.push_int(transport_type_enum.TRANSPORT_LOCAL.ordinal()); //TRANSPORT_LOCAL
		text_buf.push_int(transport_type_enum.TRANSPORT_INET_STREAM.ordinal()); //TRANSPORT_INET_STREAM

		send_message(text_buf);
	}

	public static void send_configure_ack() {
		final Text_Buf text_buf = new Text_Buf();
		text_buf.push_int(MSG_CONFIGURE_ACK);

		send_message(text_buf);
	}

	public static void send_configure_nak() {
		final Text_Buf text_buf = new Text_Buf();
		text_buf.push_int(MSG_CONFIGURE_NAK);

		send_message(text_buf);
	}

	public static void send_hc_ready() {
		final Text_Buf text_buf = new Text_Buf();
		text_buf.push_int(MSG_HC_READY);

		send_message(text_buf);
	}

	public static void send_create_req(final String componentTypeModule, final String componentTypeName,
			final String componentName, final String componentLocation, final boolean is_alive,
			final double testcase_start_time) {
		final int seconds = (int)Math.floor(testcase_start_time);
		final int miliseconds = (int)((testcase_start_time - seconds) * 1000);

		final Text_Buf text_buf = new Text_Buf();
		text_buf.push_int(MSG_CREATE_REQ);
		text_buf.push_string(componentTypeModule);
		text_buf.push_string(componentTypeName);
		text_buf.push_string(componentName);
		text_buf.push_string(componentLocation);
		text_buf.push_int( is_alive ? 1 : 0);
		text_buf.push_int(seconds);
		text_buf.push_int(miliseconds);

		send_message(text_buf);
	}

	public static void prepare_start_req(final Text_Buf text_buf, final int component_reference, final String module_name, final String function_name) {
		text_buf.push_int(MSG_START_REQ);
		text_buf.push_int(component_reference);
		text_buf.push_string(module_name);
		text_buf.push_string(function_name);
	}

	public static void send_stop_req(final int componentReference) {
		final Text_Buf text_buf = new Text_Buf();
		text_buf.push_int(MSG_STOP_REQ);
		text_buf.push_int(componentReference);

		send_message(text_buf);
	}

	public static void send_kill_req(final int componentReference) {
		final Text_Buf text_buf = new Text_Buf();
		text_buf.push_int(MSG_KILL_REQ);
		text_buf.push_int(componentReference);

		send_message(text_buf);
	}

	public static void send_is_running(final int componentReference) {
		final Text_Buf text_buf = new Text_Buf();
		text_buf.push_int(MSG_IS_RUNNING);
		text_buf.push_int(componentReference);

		send_message(text_buf);
	}

	public static void send_is_alive(final int componentReference) {
		final Text_Buf text_buf = new Text_Buf();
		text_buf.push_int(MSG_IS_ALIVE);
		text_buf.push_int(componentReference);

		send_message(text_buf);
	}

	public static void send_done_req(final int componentReference) {
		final Text_Buf text_buf = new Text_Buf();
		text_buf.push_int(MSG_DONE_REQ);
		text_buf.push_int(componentReference);

		send_message(text_buf);
	}

	public static void send_killed_req(final int componentReference) {
		final Text_Buf text_buf = new Text_Buf();
		text_buf.push_int(MSG_KILLED_REQ);
		text_buf.push_int(componentReference);

		send_message(text_buf);
	}

	public static void send_cancel_done_ack(final int componentReference) {
		final Text_Buf text_buf = new Text_Buf();
		text_buf.push_int(MSG_CANCEL_DONE_ACK);
		text_buf.push_int(componentReference);

		send_message(text_buf);
	}

	public static void send_connect_req(final int sourceComponent, final String sourcePort, final int destinationComponent, final String destinationPort) {
		final Text_Buf text_buf = new Text_Buf();
		text_buf.push_int(MSG_CONNECT_REQ);
		text_buf.push_int(sourceComponent);
		text_buf.push_string(sourcePort);
		text_buf.push_int(destinationComponent);
		text_buf.push_string(destinationPort);

		send_message(text_buf);
	}

	//FIXME extra local_port_number is not present in the core
	public static void send_connect_listen_ack_inet_stream(final String local_port, final int local_port_number, final int remote_component, final String remote_port, final InetAddress local_address) {
		final Text_Buf text_buf = new Text_Buf();
		text_buf.push_int(MSG_CONNECT_LISTEN_ACK);
		text_buf.push_string(local_port);
		text_buf.push_int(remote_component);
		text_buf.push_string(remote_port);
		text_buf.push_int(transport_type_enum.TRANSPORT_INET_STREAM.ordinal());

		if(local_address instanceof Inet4Address) {
			final byte temp[] = local_address.getAddress();
			text_buf.push_raw(2, new byte[]{2, 0});
			text_buf.push_raw(2, new byte[]{(byte)(local_port_number/256), (byte)(local_port_number%256)});
			text_buf.push_raw(temp.length, temp);
			text_buf.push_raw(8, new byte[8]);
		} else if (local_address instanceof Inet6Address) {
			final Inet6Address localipv6_address = getIPv6Address(local_address);
			final byte temp[] = localipv6_address.getAddress();
			text_buf.push_raw(2, new byte[]{2, 3});
			text_buf.push_raw(2, new byte[]{(byte)(local_port_number/256), (byte)(local_port_number%256)});
			text_buf.push_raw(temp.length, temp);
			text_buf.push_int(localipv6_address.getScopeId());
		}

		//FIXME implement

		send_message(text_buf);
	}

	public static void send_connected(final String local_port, final int remote_component, final String remote_port){
		final Text_Buf text_buf = new Text_Buf();
		text_buf.push_int(MSG_CONNECTED);
		text_buf.push_string(local_port);
		text_buf.push_int(remote_component);
		text_buf.push_string(remote_port);

		send_message(text_buf);
	}

	// in the command line receives variable argument list
	public static void send_connect_error(final String local_port, final int remote_component, final String remote_port, final String message){
		final Text_Buf text_buf = new Text_Buf();
		text_buf.push_int(MSG_CONNECT_ERROR);
		text_buf.push_string(local_port);
		text_buf.push_int(remote_component);
		text_buf.push_string(remote_port);
		text_buf.push_string(message);

		send_message(text_buf);
	}

	public static void send_disconnect_req(final int sourceComponent, final String sourcePort, final int destinationComponent, final String destinationPort) {
		final Text_Buf text_buf = new Text_Buf();
		text_buf.push_int(MSG_DISCONNECT_REQ);
		text_buf.push_int(sourceComponent);
		text_buf.push_string(sourcePort);
		text_buf.push_int(destinationComponent);
		text_buf.push_string(destinationPort);

		send_message(text_buf);
	}

	public static void send_disconnected(final String localPort, final int remoteComponent, final String remotePort) {
		final Text_Buf text_buf = new Text_Buf();
		text_buf.push_int(MSG_DISCONNECTED);
		text_buf.push_string(localPort);
		text_buf.push_int(remoteComponent);
		text_buf.push_string(remotePort);

		send_message(text_buf);
	}

	public static void send_map_req(final int sourceComponent, final String sourcePort, final String systemPort, final Map_Params params, final boolean translation) {
		final Text_Buf text_buf = new Text_Buf();
		text_buf.push_int(MSG_MAP_REQ);
		text_buf.push_int(sourceComponent);
		text_buf.push_int(translation ? 1 : 0);
		text_buf.push_string(sourcePort);
		text_buf.push_string(systemPort);
		final int nof_params = params.get_nof_params();
		text_buf.push_int(nof_params);
		for (int i = 0; i < nof_params; i++) {
			text_buf.push_string(params.get_param(i).get_value().toString());
		}

		send_message(text_buf);
	}

	public static void send_mapped(final String localPort, final String systemPort, final Map_Params params, final boolean translation) {
		final Text_Buf text_buf = new Text_Buf();
		text_buf.push_int(MSG_MAPPED);
		text_buf.push_int(translation ? 1 : 0);
		text_buf.push_string(localPort);
		text_buf.push_string(systemPort);
		final int nof_params = params.get_nof_params();
		text_buf.push_int(nof_params);
		for (int i = 0; i < nof_params; i++) {
			text_buf.push_string(params.get_param(i).get_value().toString());
		}

		send_message(text_buf);
	}

	public static void send_unmap_req(final int sourceComponent, final String sourcePort, final String systemPort, final Map_Params params, final boolean translation) {
		final Text_Buf text_buf = new Text_Buf();
		text_buf.push_int(MSG_UNMAP_REQ);
		text_buf.push_int(sourceComponent);
		text_buf.push_int(translation ? 1 : 0);
		text_buf.push_string(sourcePort);
		text_buf.push_string(systemPort);
		final int nof_params = params.get_nof_params();
		text_buf.push_int(nof_params);
		for (int i = 0; i < nof_params; i++) {
			text_buf.push_string(params.get_param(i).get_value().toString());
		}

		send_message(text_buf);
	}

	public static void send_unmapped(final String localPort, final String systemPort, final Map_Params params, final boolean translation) {
		final Text_Buf text_buf = new Text_Buf();
		text_buf.push_int(MSG_UNMAPPED);
		text_buf.push_int(translation ? 1 : 0);
		text_buf.push_string(localPort);
		text_buf.push_string(systemPort);
		final int nof_params = params.get_nof_params();
		text_buf.push_int(nof_params);
		for (int i = 0; i < nof_params; i++) {
			text_buf.push_string(params.get_param(i).get_value().toString());
		}

		send_message(text_buf);
	}

	public static void send_mtc_created() {
		final Text_Buf text_buf = new Text_Buf();
		text_buf.push_int(MSG_MTC_CREATED);

		send_message(text_buf);
	}

	public static void send_testcase_started(final String testcaseModule, final String testcaseName, final String mtc_comptype_module,
			final String mtc_comptype_name, final String system_comptype_module, final String system_comptype_name) {
		final Text_Buf text_buf = new Text_Buf();
		text_buf.push_int(MSG_TESTCASE_STARTED);
		text_buf.push_string(testcaseModule);
		text_buf.push_string(testcaseName);
		text_buf.push_string(mtc_comptype_module);
		text_buf.push_string(mtc_comptype_name);
		text_buf.push_string(system_comptype_module);
		text_buf.push_string(system_comptype_name);

		send_message(text_buf);
	}

	public static void send_testcase_finished(final TitanVerdictType.VerdictTypeEnum finalVerdict, final String reason) {
		final Text_Buf text_buf = new Text_Buf();
		text_buf.push_int(MSG_TESTCASE_FINISHED);
		text_buf.push_int(finalVerdict.ordinal());
		text_buf.push_string(reason);

		send_message(text_buf);
	}

	public static void send_mtc_ready() {
		final Text_Buf text_buf = new Text_Buf();
		text_buf.push_int(MSG_MTC_READY);

		send_message(text_buf);
	}

	public static void send_ptc_created(final int component_reference) {
		final Text_Buf text_buf = new Text_Buf();
		text_buf.push_int(MSG_PTC_CREATED);
		text_buf.push_int(component_reference);

		send_message(text_buf);
	}

	public static void prepare_stopped(final Text_Buf text_buf, final TitanVerdictType.VerdictTypeEnum final_verdict, final String return_type, final String reason) {
		text_buf.push_int(MSG_STOPPED);
		text_buf.push_int(final_verdict.getValue());
		text_buf.push_string(reason);
		text_buf.push_string(return_type);
	}

	public static void send_stopped(final TitanVerdictType.VerdictTypeEnum final_verdict, final String reason) {
		final Text_Buf text_buf = new Text_Buf();
		text_buf.push_int(MSG_STOPPED);
		text_buf.push_int(final_verdict.ordinal());
		text_buf.push_string(reason);
		// add an empty return type
		text_buf.push_string(null);

		send_message(text_buf);
	}

	public static void prepare_stopped_killed(final Text_Buf text_buf, final TitanVerdictType.VerdictTypeEnum final_verdict, final String return_type, final String reason) {
		text_buf.push_int(MSG_STOPPED_KILLED);
		text_buf.push_int(final_verdict.getValue());
		text_buf.push_string(reason);
		text_buf.push_string(return_type);
	}

	public static void send_stopped_killed(final TitanVerdictType.VerdictTypeEnum final_verdict, final String reason) {
		final Text_Buf text_buf = new Text_Buf();
		text_buf.push_int(MSG_STOPPED_KILLED);
		text_buf.push_int(final_verdict.getValue());
		text_buf.push_string(reason);
		// add an empty return type
		text_buf.push_string(null);

		send_message(text_buf);
	}

	public static void send_killed(final TitanVerdictType.VerdictTypeEnum final_verdict, final String reason) {
		final Text_Buf text_buf = new Text_Buf();
		text_buf.push_int(MSG_KILLED);
		text_buf.push_int(final_verdict.getValue());
		text_buf.push_string(reason);
		// add an empty return type
		text_buf.push_string(null);

		send_message(text_buf);
	}

	public static boolean send_log(final int seconds, final int microseconds, final int event_severity, final String message_text) {
		if (is_connected.get()) {
			final Text_Buf text_buf = new Text_Buf();
			text_buf.push_int(MSG_LOG);
			text_buf.push_int(seconds);
			text_buf.push_int(microseconds);
			text_buf.push_int(event_severity);
			text_buf.push_int(message_text.length());
			final byte messageBytes[] = message_text.getBytes();
			text_buf.push_raw(messageBytes.length, messageBytes);
			send_message(text_buf);

			/* If an ERROR message (indicating a version mismatch) arrives from MC
			   in state HC_IDLE (i.e. before CONFIGURE) it shall be
			   printed to the console as well. */
			return TTCN_Runtime.get_state() != executorStateEnum.HC_IDLE;
		} else {
			switch (TTCN_Runtime.get_state()) {
			case HC_EXIT:
			case MTC_INITIAL:
			case MTC_EXIT:
			case PTC_INITIAL:
			case PTC_EXIT:
				/* Do not print the first/last few lines of logs to the console even if ConsoleMask is set to LOG_ALL */
				return true;
			default:
				return false;
			}
		}
	}

	public static void send_error(final String message) {
		final Text_Buf text_buf = new Text_Buf();
		text_buf.push_int(MSG_ERROR);
		text_buf.push_string(message);

		send_message(text_buf);
	}

	public static void send_message(final Text_Buf text_buf) {
		if (!is_connected.get()) {
			throw new TtcnError("Trying to send a message to MC, but the control connection is down.");
		}

		text_buf.calculate_length();
		final byte msg[] = text_buf.get_data();
		final ByteBuffer buffer = ByteBuffer.allocate(text_buf.get_len());
		final byte temp_msg[] = new byte[text_buf.get_len()];
		System.arraycopy(msg, text_buf.get_begin(), temp_msg, 0, text_buf.get_len());
		buffer.put(temp_msg);
		buffer.flip();

		final SocketChannel localChannel = mc_socketchannel.get();
		try {
			while (buffer.hasRemaining()) {
				localChannel.write(buffer);
			}
		} catch (IOException e) {
			close_mc_connection();

			final StringWriter error = new StringWriter();
			e.printStackTrace(new PrintWriter(error));
	
			TTCN_Logger.begin_event(Severity.ERROR_UNQUALIFIED);
			TTCN_Logger.log_event_str("Dynamic test case error: ");
			TTCN_Logger.log_event_str(error.toString());
			TTCN_Logger.end_event();
			throw new TtcnError("Sending data on the control connection to MC failed.");
		}
	}

	private static void process_configure(final int msg_end, final boolean to_mtc) {
		switch (TTCN_Runtime.get_state()) {
		case HC_IDLE:
		case HC_ACTIVE:
		case HC_OVERLOADED:
			if (!to_mtc) {
				break;
			}
		case MTC_IDLE:
			if (to_mtc) {
				break;
			}
		default:
			incoming_buf.get().cut_message();
			send_error("Message CONFIGURE arrived in invalid state.");
			return;
		}

		TTCN_Runtime.set_state(to_mtc ? executorStateEnum.MTC_CONFIGURING : executorStateEnum.HC_CONFIGURING);
		TTCN_Logger.log_configdata(ExecutorConfigdata_reason.enum_type.received__from__mc, null);

		final Text_Buf local_incoming_buf = incoming_buf.get();
		final int config_str_len = local_incoming_buf.pull_int().get_int();
		final int config_str_begin = local_incoming_buf.get_pos();
		if (config_str_begin + config_str_len != msg_end) {
			local_incoming_buf.cut_message();
			send_error("Malformed message CONFIGURE was received.");
			return;
		}

		final String config_str;
		if (config_str_len == 0) {
			config_str = "";
		} else {
			final byte[] config_bytes = new byte[config_str_len];
			final byte[] incoming_data = local_incoming_buf.get_data();
			System.arraycopy(incoming_data, local_incoming_buf.get_begin() + config_str_begin, config_bytes, 0, config_str_len);
			config_str = new String(config_bytes);
		}

		final CfgAnalyzer cfgAnalyzer = new CfgAnalyzer();
		boolean success = !cfgAnalyzer.parse(config_str);
		TTCN_Logger.open_file();
		if (success) {
			try {
				Module_List.post_init_modules();
			} catch (TtcnError error) {
				TTCN_Logger.log_executor_runtime(TitanLoggerApi.ExecutorRuntime_reason.enum_type.initialization__of__modules__failed);
				success = false;
			}
			
		} else {
			TTCN_Logger.log_configdata(ExecutorConfigdata_reason.enum_type.processing__failed, null);
		}
		if (success) {
			send_configure_ack();
			TTCN_Runtime.set_state(to_mtc ? executorStateEnum.MTC_IDLE : executorStateEnum.HC_ACTIVE);
			TTCN_Logger.log_configdata(ExecutorConfigdata_reason.enum_type.processing__succeeded, null);
		} else {
			send_configure_nak();
			TTCN_Runtime.set_state(to_mtc ? executorStateEnum.MTC_IDLE : executorStateEnum.HC_IDLE);
		}

		local_incoming_buf.cut_message();
	}

	private static void process_create_mtc() {
		incoming_buf.get().cut_message();
		TTCN_Runtime.process_create_mtc();
	}

	private static void process_create_ptc() {
		final Text_Buf local_incoming_buf = incoming_buf.get();

		final int component_reference = local_incoming_buf.pull_int().get_int();
		if (component_reference < TitanComponent.FIRST_PTC_COMPREF) {
			local_incoming_buf.cut_message();
			send_error(MessageFormat.format("Message CREATE_PTC refers to invalid component reference {0}.", component_reference));
			return;
		}

		final String component_module_name = local_incoming_buf.pull_string();
		final String component_definition_name = local_incoming_buf.pull_string();
		final String system_module_name = local_incoming_buf.pull_string();
		final String system_definition_name = local_incoming_buf.pull_string();
		if (component_module_name == null || component_definition_name == null || system_module_name == null || system_definition_name == null) {
			send_error(MessageFormat.format("Message CREATE_PTC with component reference {0} contains an invalid component type or system type.", component_reference));
		}

		final String component_name = local_incoming_buf.pull_string();
		final boolean is_alive = local_incoming_buf.pull_int().get_int() == 0 ? false : true;
		final String testcase_module_name = local_incoming_buf.pull_string();
		final String testcase_definition_name = local_incoming_buf.pull_string();
		final int seconds = local_incoming_buf.pull_int().get_int();
		final int milliSeconds = local_incoming_buf.pull_int().get_int();
		local_incoming_buf.cut_message();

		final double start_time = seconds + milliSeconds / 1000.0;

		TTCN_Runtime.process_create_ptc(component_reference, component_module_name, component_definition_name, system_module_name, system_definition_name, component_name, is_alive, testcase_module_name, testcase_definition_name, start_time);
	}

	private static void process_kill_process() {
		final Text_Buf local_incoming_buf = incoming_buf.get();

		final int component_reference = local_incoming_buf.pull_int().get_int();
		local_incoming_buf.cut_message();

		TTCN_Runtime.process_kill_process(component_reference);
	}

	private static void process_exit_hc() {
		incoming_buf.get().cut_message();
		TTCN_Logger.log_executor_runtime(TitanLoggerApi.ExecutorRuntime_reason.enum_type.exit__requested__from__mc__hc);
		TTCN_Runtime.set_state(executorStateEnum.HC_EXIT);
	}

	private static void process_create_ack() {
		final Text_Buf local_incoming_buf = incoming_buf.get();

		final int component_reference = local_incoming_buf.pull_int().get_int();
		local_incoming_buf.cut_message();

		TTCN_Runtime.process_create_ack(component_reference);
	}

	private static void process_start_ack() {
		incoming_buf.get().cut_message();

		switch (TTCN_Runtime.get_state()) {
		case MTC_START:
			TTCN_Runtime.set_state(executorStateEnum.MTC_TESTCASE);
			break;
		case MTC_TERMINATING_TESTCASE:
			break;
		case PTC_START:
			TTCN_Runtime.set_state(executorStateEnum.PTC_FUNCTION);
			break;
		default:
			throw new TtcnError("Internal error: Message START_ACK arrived in invalid state.");
		}
	}

	private static void process_stop() {
		incoming_buf.get().cut_message();

		switch (TTCN_Runtime.get_state()) {
		case MTC_IDLE:
			TTCN_Logger.log_executor_runtime(TitanLoggerApi.ExecutorRuntime_reason.enum_type.stop__was__requested__from__mc__ignored__on__idle__mtc);
			break;
		case MTC_PAUSED:
			TTCN_Logger.log_executor_runtime(TitanLoggerApi.ExecutorRuntime_reason.enum_type.stop__was__requested__from__mc);
			TTCN_Runtime.set_state(executorStateEnum.MTC_TERMINATING_EXECUTION);
			break;
		case PTC_IDLE:
		case PTC_STOPPED:
			TTCN_Logger.log_executor_runtime(TitanLoggerApi.ExecutorRuntime_reason.enum_type.stop__was__requested__from__mc__ignored__on__idle__ptc);
			break;
		case PTC_EXIT:
			break;
		default:
			TTCN_Logger.log_executor_runtime(TitanLoggerApi.ExecutorRuntime_reason.enum_type.stop__was__requested__from__mc);
			TTCN_Runtime.stop_execution();
			break;
		}
	}

	private static void process_stop_ack() {
		incoming_buf.get().cut_message();

		switch (TTCN_Runtime.get_state()) {
		case MTC_STOP:
			TTCN_Runtime.set_state(executorStateEnum.MTC_TESTCASE);
			break;
		case MTC_TERMINATING_TESTCASE:
			break;
		case PTC_STOP:
			TTCN_Runtime.set_state(executorStateEnum.PTC_FUNCTION);
			break;
		default:
			throw new TtcnError("Internal error: Message STOP_ACK arrived in invalid state.");
		}
	}

	private static void process_kill_ack() {
		incoming_buf.get().cut_message();

		switch (TTCN_Runtime.get_state()) {
		case MTC_KILL:
			TTCN_Runtime.set_state(executorStateEnum.MTC_TESTCASE);
			break;
		case MTC_TERMINATING_TESTCASE:
			break;
		case PTC_KILL:
			TTCN_Runtime.set_state(executorStateEnum.PTC_FUNCTION);
			break;
		default:
			throw new TtcnError("Internal error: Message KILL_ACK arrived in invalid state.");
		}
	}

	private static void process_running() {
		final Text_Buf local_incoming_buf = incoming_buf.get();

		final boolean answer = local_incoming_buf.pull_int().get_int() == 0 ? false: true;
		local_incoming_buf.cut_message();

		TTCN_Runtime.process_running(answer);
	}

	private static void process_alive() {
		final Text_Buf local_incoming_buf = incoming_buf.get();

		final boolean answer = local_incoming_buf.pull_int().get_int() == 0 ? false: true;
		local_incoming_buf.cut_message();

		TTCN_Runtime.process_alive(answer);
	}

	private static void process_done_ack(final int msg_end) {
		final Text_Buf local_incoming_buf = incoming_buf.get();

		final boolean answer = local_incoming_buf.pull_int().get_int() == 0 ? false: true;
		final int verdict_int = local_incoming_buf.pull_int().get_int();
		final VerdictTypeEnum ptc_verdict = TitanVerdictType.VerdictTypeEnum.values()[verdict_int];
		final String return_type = local_incoming_buf.pull_string();
		final int return_value_begin = local_incoming_buf.get_pos();

		try {
			TTCN_Runtime.process_done_ack(answer, ptc_verdict, return_type, local_incoming_buf.get_data(), msg_end - return_value_begin, local_incoming_buf.get_begin(), return_value_begin);
		} finally {
			local_incoming_buf.cut_message();
		}
	}

	private static void process_killed_ack() {
		final Text_Buf local_incoming_buf = incoming_buf.get();

		final boolean answer = local_incoming_buf.pull_int().get_int() == 0 ? false: true;
		local_incoming_buf.cut_message();

		TTCN_Runtime.process_killed_ack(answer);
	}

	private static void process_cancel_done_mtc() {
		final Text_Buf local_incoming_buf = incoming_buf.get();

		final int component_reference = local_incoming_buf.pull_int().get_int();
		final boolean cancel_any = local_incoming_buf.pull_int().get_int() == 0 ? false : true;
		local_incoming_buf.cut_message();

		TTCN_Runtime.cancel_component_done(component_reference);
		if (cancel_any) {
			TTCN_Runtime.cancel_component_done(TitanComponent.ANY_COMPREF);
		}
		send_cancel_done_ack(component_reference);
	}

	private static void process_cancel_done_ptc() {
		final Text_Buf local_incoming_buf = incoming_buf.get();

		final int component_reference = local_incoming_buf.pull_int().get_int();
		local_incoming_buf.cut_message();

		TTCN_Runtime.cancel_component_done(component_reference);
		send_cancel_done_ack(component_reference);
	}

	private static void process_component_status_mtc(final int msg_end) {
		final Text_Buf local_incoming_buf = incoming_buf.get();

		final int component_reference = local_incoming_buf.pull_int().get_int();
		final boolean is_done = local_incoming_buf.pull_int().get_int() == 0 ? false: true;
		final boolean is_killed = local_incoming_buf.pull_int().get_int() == 0 ? false: true;
		final boolean is_any_done = local_incoming_buf.pull_int().get_int() == 0 ? false: true;
		final boolean is_all_done = local_incoming_buf.pull_int().get_int() == 0 ? false: true;
		final boolean is_any_killed = local_incoming_buf.pull_int().get_int() == 0 ? false: true;
		final boolean is_all_killed = local_incoming_buf.pull_int().get_int() == 0 ? false: true;
		if (is_done) {
			// the return type and value are valid
			final int verdict_int = local_incoming_buf.pull_int().get_int();
			final VerdictTypeEnum ptc_verdict = TitanVerdictType.VerdictTypeEnum.values()[verdict_int];
			final String return_type = local_incoming_buf.pull_string();
			final int return_value_begin = local_incoming_buf.get_pos();
			try {
				TTCN_Runtime.set_component_done(component_reference, ptc_verdict, return_type, local_incoming_buf.get_data(), msg_end - return_value_begin, local_incoming_buf.get_begin(), return_value_begin);
			} catch (TtcnError error) {
				local_incoming_buf.cut_message();
				throw error;
			}
		}

		if (is_killed) {
			TTCN_Runtime.set_component_killed(component_reference);
		}
		if (is_any_done) {
			TTCN_Runtime.set_component_done(TitanComponent.ANY_COMPREF, VerdictTypeEnum.NONE, null, null, 0, 0, 0);
		}
		if (is_all_done) {
			TTCN_Runtime.set_component_done(TitanComponent.ALL_COMPREF, VerdictTypeEnum.NONE, null, null, 0, 0, 0);
		}
		if (is_any_killed) {
			TTCN_Runtime.set_component_killed(TitanComponent.ANY_COMPREF);
		}
		if (is_all_killed) {
			TTCN_Runtime.set_component_killed(TitanComponent.ALL_COMPREF);
		}

		local_incoming_buf.cut_message();
		if (!is_done && !is_killed && (component_reference != TitanComponent.NULL_COMPREF ||
				(!is_any_done && !is_all_done && !is_any_killed && !is_all_killed))) {
			throw new TtcnError("Internal error: Malformed COMPONENT_STATUS message was received.");
		}
	}

	private static void process_component_status_ptc(final int msg_end) {
		final Text_Buf local_incoming_buf = incoming_buf.get();

		final int component_reference = local_incoming_buf.pull_int().get_int();
		final boolean is_done = local_incoming_buf.pull_int().get_int() == 0 ? false: true;
		final boolean is_killed = local_incoming_buf.pull_int().get_int() == 0 ? false: true;
		if (is_done) {
			// the return type and value are valid
			final int verdict_int = local_incoming_buf.pull_int().get_int();
			final VerdictTypeEnum ptc_verdict = TitanVerdictType.VerdictTypeEnum.values()[verdict_int];
			final String return_type = local_incoming_buf.pull_string();
			final int return_value_begin = local_incoming_buf.get_pos();
			try {
				TTCN_Runtime.set_component_done(component_reference, ptc_verdict, return_type, local_incoming_buf.get_data(), msg_end - return_value_begin, local_incoming_buf.get_begin(), return_value_begin);
			} catch (TtcnError error) {
				local_incoming_buf.cut_message();
				throw error;
			}
		}

		if (is_killed) {
			TTCN_Runtime.set_component_killed(component_reference);
		}

		local_incoming_buf.cut_message();
		if (!is_done && !is_killed) {
			throw new TtcnError("Internal error: Malformed COMPONENT_STATUS message was received.");
		}
	}

	private static void process_connect_listen() {
		final Text_Buf local_incoming_buf = incoming_buf.get();

		final String local_port = local_incoming_buf.pull_string();
		final int remote_component = local_incoming_buf.pull_int().get_int();
		final String remote_component_name = local_incoming_buf.pull_string();
		final String remote_port = local_incoming_buf.pull_string();
		final int temp_transport_type = local_incoming_buf.pull_int().get_int();

		local_incoming_buf.cut_message();

		if (remote_component != TitanComponent.MTC_COMPREF && TitanComponent.self.get().get_component() != remote_component) {
			TitanComponent.register_component_name(remote_component, remote_component_name);
		}

		final transport_type_enum transport_type = transport_type_enum.values()[temp_transport_type];
		TitanPort.process_connect_listen(local_port, remote_component, remote_port, transport_type);

		local_incoming_buf.cut_message();
	}

	private static void process_connect() {
		final Text_Buf local_incoming_buf = incoming_buf.get();

		final String local_port = local_incoming_buf.pull_string();
		final int remote_component = local_incoming_buf.pull_int().get_int();
		final String remote_component_name = local_incoming_buf.pull_string();
		final String remote_port = local_incoming_buf.pull_string();
		final int temp_transport_type = local_incoming_buf.pull_int().get_int();

		try {
			if (remote_component != TitanComponent.MTC_COMPREF && TitanComponent.self.get().get_component() != remote_component) {
				TitanComponent.register_component_name(remote_component, remote_component_name);
			}

			final transport_type_enum transport_type = transport_type_enum.values()[temp_transport_type];
			TitanPort.process_connect(local_port, remote_component, remote_port, transport_type, incoming_buf.get());
		} finally {
			local_incoming_buf.cut_message();
		}
	}

	private static void process_connect_ack() {
		incoming_buf.get().cut_message();

		switch (TTCN_Runtime.get_state()) {
		case MTC_CONNECT:
			TTCN_Runtime.set_state(executorStateEnum.MTC_TESTCASE);
			break;
		case MTC_TERMINATING_TESTCASE:
			break;
		case PTC_CONNECT:
			TTCN_Runtime.set_state(executorStateEnum.PTC_FUNCTION);
			break;
		default:
			throw new TtcnError("Internal error: Message CONNECT_ACK arrived in invalid state.");
		}
	}

	private static void process_disconnect() {
		final Text_Buf local_incoming_buf = incoming_buf.get();

		final String local_port = local_incoming_buf.pull_string();
		final int remote_component = local_incoming_buf.pull_int().get_int();
		final String remote_port = local_incoming_buf.pull_string();

		local_incoming_buf.cut_message();

		TitanPort.process_disconnect(local_port, remote_component, remote_port);
	}

	private static void process_disconnect_ack() {
		incoming_buf.get().cut_message();

		switch (TTCN_Runtime.get_state()) {
		case MTC_DISCONNECT:
			TTCN_Runtime.set_state(executorStateEnum.MTC_TESTCASE);
			break;
		case MTC_TERMINATING_TESTCASE:
			break;
		case PTC_DISCONNECT:
			TTCN_Runtime.set_state(executorStateEnum.PTC_FUNCTION);
			break;
		default:
			throw new TtcnError("Internal error: Message DISCONNECT_ACK arrived in invalid state.");
		}
	}

	private static void process_map() {
		final Text_Buf local_incoming_buf = incoming_buf.get();

		final boolean translation = local_incoming_buf.pull_int().get_int() == 0 ? false: true;
		final String local_port = local_incoming_buf.pull_string();
		final String system_port = local_incoming_buf.pull_string();
		final int nof_params = local_incoming_buf.pull_int().get_int();
		final Map_Params params = new Map_Params(nof_params);
		for (int i = 0; i < nof_params; i++) {
			final String par = local_incoming_buf.pull_string();
			params.set_param(i, new TitanCharString(par));
		}

		local_incoming_buf.cut_message();

		TitanPort.map_port(local_port, system_port, params, false);
		if (translation) {
			TitanPort.map_port(local_port, system_port, params, true);
		}
		if (!TTCN_Runtime.is_single()) {
			if (translation) {
				send_mapped(system_port, local_port, params, translation);
			} else {
				send_mapped(local_port, system_port, params, translation);
			}
		}
	}

	private static void process_map_ack() {
		final Text_Buf local_incoming_buf = incoming_buf.get();
		final int nof_params = local_incoming_buf.pull_int().get_int();
		final Map_Params local_map_params = TitanPort.map_params_cache.get();
		local_map_params.reset(nof_params);
		for (int i = 0; i < nof_params; i++) {
			final String par = local_incoming_buf.pull_string();
			local_map_params.set_param(i, new TitanCharString(par));
		}
		local_incoming_buf.cut_message();

		switch (TTCN_Runtime.get_state()) {
		case MTC_MAP:
			TTCN_Runtime.set_state(executorStateEnum.MTC_TESTCASE);
			break;
		case MTC_TERMINATING_TESTCASE:
			break;
		case PTC_MAP:
			TTCN_Runtime.set_state(executorStateEnum.PTC_FUNCTION);
			break;
		default:
			throw new TtcnError("Internal error: Message MAP_ACK arrived in invalid state.");
		}
	}

	private static void process_unmap() {
		final Text_Buf local_incoming_buf = incoming_buf.get();

		final boolean translation = local_incoming_buf.pull_int().get_int() == 0 ? false: true;
		final String local_port = local_incoming_buf.pull_string();
		final String system_port = local_incoming_buf.pull_string();
		final int nof_params = local_incoming_buf.pull_int().get_int();
		final Map_Params params = new Map_Params(nof_params);
		for (int i = 0; i < nof_params; i++) {
			final String par = local_incoming_buf.pull_string();
			params.set_param(i, new TitanCharString(par));
		}

		local_incoming_buf.cut_message();

		TitanPort.unmap_port(local_port, system_port, params, false);
		if (translation) {
			TitanPort.unmap_port(local_port, system_port, params, true);
		}
		if (!TTCN_Runtime.is_single()) {
			if (translation) {
				send_unmapped(system_port, local_port, params, translation);
			} else {
				send_unmapped(local_port, system_port, params, translation);
			}
		}
	}

	private static void process_unmap_ack() {
		final Text_Buf local_incoming_buf = incoming_buf.get();
		final int nof_params = local_incoming_buf.pull_int().get_int();
		final Map_Params local_map_params = TitanPort.map_params_cache.get();
		local_map_params.reset(nof_params);
		for (int i = 0; i < nof_params; i++) {
			final String par = local_incoming_buf.pull_string();
			local_map_params.set_param(i, new TitanCharString(par));
		}
		local_incoming_buf.cut_message();

		switch (TTCN_Runtime.get_state()) {
		case MTC_UNMAP:
			TTCN_Runtime.set_state(executorStateEnum.MTC_TESTCASE);
			break;
		case MTC_TERMINATING_TESTCASE:
			break;
		case PTC_UNMAP:
			TTCN_Runtime.set_state(executorStateEnum.PTC_FUNCTION);
			break;
		default:
			throw new TtcnError("Internal error: Message UNMAP_ACK arrived in invalid state.");
		}
	}

	private static void process_execute_control() {
		final Text_Buf local_incoming_buf = incoming_buf.get();

		final String module_name = local_incoming_buf.pull_string();
		local_incoming_buf.cut_message();

		if (TTCN_Runtime.get_state() != executorStateEnum.MTC_IDLE) {
			throw new TtcnError("Internal error: Message EXECUTE_CONTROL arrived in invalid state.");
		}

		TTCN_Logger.log(Severity.PARALLEL_UNQUALIFIED, MessageFormat.format("Executing control part of module {0}.", module_name));

		TTCN_Runtime.set_state(executorStateEnum.MTC_CONTROLPART);

		try {
			Module_List.execute_control(module_name);
		} catch (TC_End TC_end) {
			//no operation needed
		} catch (TtcnError error) {
			//no operation needed
		}

		if (is_connected.get()) {
			send_mtc_ready();
			TTCN_Runtime.set_state(executorStateEnum.MTC_IDLE);
		} else {
			TTCN_Runtime.set_state(executorStateEnum.MTC_EXIT);
		}
	}

	private static void process_execute_testcase() {
		final Text_Buf local_incoming_buf = incoming_buf.get();

		final String module_name = local_incoming_buf.pull_string();
		final String testcase_name = local_incoming_buf.pull_string();
		local_incoming_buf.cut_message();

		if (TTCN_Runtime.get_state() != executorStateEnum.MTC_IDLE) {
			throw new TtcnError("Internal error: Message EXECUTE_TESTCASE arrived in invalid state."); 
		}

		TTCN_Logger.log_testcase_exec(testcase_name, module_name);
		TTCN_Runtime.set_state(executorStateEnum.MTC_CONTROLPART);

		try {
			if (testcase_name != null && testcase_name.length() > 0) {
				Module_List.execute_testcase(module_name, testcase_name);
			} else {
				Module_List.execute_all_testcases(module_name);
			}
		} catch (TC_End TC_end) {
			//no operation needed
		} catch (TtcnError error) {
			//no operation needed
		}

		if (is_connected.get()) {
			send_mtc_ready();
			TTCN_Runtime.set_state(executorStateEnum.MTC_IDLE);
		} else {
			TTCN_Runtime.set_state(executorStateEnum.MTC_EXIT);
		}
	}

	private static void process_ptc_verdict() {
		final Text_Buf local_incoming_buf = incoming_buf.get();

		TTCN_Runtime.process_ptc_verdict(local_incoming_buf);
		local_incoming_buf.cut_message();
	}

	private static void process_exit_mtc() {
		incoming_buf.get().cut_message();
		TTCN_Runtime.log_verdict_statistics();
		TTCN_Logger.log_executor_runtime(TitanLoggerApi.ExecutorRuntime_reason.enum_type.exit__requested__from__mc__mtc);
		TTCN_Runtime.set_state(executorStateEnum.MTC_EXIT);
	}

	private static void process_start() {
		final Text_Buf local_incoming_buf = incoming_buf.get();

		final String module_name = local_incoming_buf.pull_string();
		final String definition_name = local_incoming_buf.pull_string();

		if (module_name == null || definition_name == null) {
			local_incoming_buf.cut_message();

			throw new TtcnError("Internal error: Message START contains an invalid function name.");
		}

		TTCN_Runtime.start_function(module_name, definition_name, local_incoming_buf);
	}

	private static void process_kill() {
		incoming_buf.get().cut_message();

		TTCN_Runtime.process_kill();
	}

	private static void process_error() {
		final Text_Buf local_incoming_buf = incoming_buf.get();

		final String error_string = local_incoming_buf.pull_string();

		local_incoming_buf.cut_message();

		throw new TtcnError("Error message was received from MC : " + error_string);
	}

	private static void process_unsupported_message(final int msg_type, final int msg_end) {
		final Text_Buf local_incoming_buf = incoming_buf.get();

		TTCN_Logger.begin_event(Severity.WARNING_UNQUALIFIED);
		TTCN_Logger.log_event_str(MessageFormat.format("Unsupported message was received from MC: type (decimal): {0}, data (hexadecimal): ", msg_type));

		final byte[] data = local_incoming_buf.get_data();
		final int begin = local_incoming_buf.get_begin();
		for (int i = local_incoming_buf.get_pos(); i < msg_end; i++) {
			TTCN_Logger.log_octet((char)data[begin + i]);
		}
		TTCN_Logger.end_event();
		local_incoming_buf.cut_message();
	}

	private static void process_debug_command() {
		final Text_Buf local_incoming_buf = incoming_buf.get();

		final int command = local_incoming_buf.pull_int().get_int();
		final int argument_count = local_incoming_buf.pull_int().get_int();
		//FIXME process the arguments properly
		if (argument_count > 0) {
			for (int i = 0; i < argument_count; i++) {
				local_incoming_buf.pull_string();
			}
		}
		local_incoming_buf.cut_message();
		//FIXME implement execute_command
	}

	//Private function to convert InetAddress to Inet6Address.
	private static Inet6Address getIPv6Address(final InetAddress address) {
		return (Inet6Address)address;
	}
}
