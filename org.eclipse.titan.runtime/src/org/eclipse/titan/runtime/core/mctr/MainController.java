/******************************************************************************
 * Copyright (c) 2000-2021 Ericsson Telecom AB
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/org/documents/epl-2.0/EPL-2.0.html
 ******************************************************************************/
package org.eclipse.titan.runtime.core.mctr;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.StandardSocketOptions;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

import org.eclipse.titan.runtime.core.NetworkHandler;
import org.eclipse.titan.runtime.core.TTCN_Communication.transport_type_enum;
import org.eclipse.titan.runtime.core.TTCN_Logger;
import org.eclipse.titan.runtime.core.TTCN_Runtime;
import org.eclipse.titan.runtime.core.Text_Buf;
import org.eclipse.titan.runtime.core.TitanCharString;
import org.eclipse.titan.runtime.core.TitanComponent;
import org.eclipse.titan.runtime.core.TitanPort.Map_Params;
import org.eclipse.titan.runtime.core.TitanVerdictType;
import org.eclipse.titan.runtime.core.TitanVerdictType.VerdictTypeEnum;
import org.eclipse.titan.runtime.core.TtcnError;

/**
 *
 * @author Bianka Bekefi
 * @author Kristof Szabados
 */

public class MainController {

	// TODO As these are shared with the TTCN_Communication class maybe they could be extracted
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

	/** For representing the global state of MC */
	public static enum mcStateEnum {
		MC_INACTIVE,
		MC_LISTENING,
		MC_LISTENING_CONFIGURED,
		MC_HC_CONNECTED,
		MC_CONFIGURING,
		MC_ACTIVE,
		MC_SHUTDOWN,
		MC_CREATING_MTC,
		MC_READY,
		MC_TERMINATING_MTC,
		MC_EXECUTING_CONTROL,
		MC_EXECUTING_TESTCASE,
		MC_TERMINATING_TESTCASE,
		MC_PAUSED,
		MC_RECONFIGURING
	}

	/** Possible states of a HC */
	private static enum hc_state_enum {
		HC_IDLE, HC_CONFIGURING, HC_ACTIVE, HC_OVERLOADED, HC_CONFIGURING_OVERLOADED, HC_EXITING, HC_DOWN
	}

	/** Possible states of a port connection or mapping */
	private static enum conn_state_enum {
		CONN_LISTENING, CONN_CONNECTING, CONN_CONNECTED, CONN_DISCONNECTING, CONN_MAPPING, CONN_MAPPED, CONN_UNMAPPING
	}

	/** Possible states of a TC (MTC or PTC) */
	public static enum tc_state_enum {
		TC_INITIAL,
		TC_IDLE,
		TC_CREATE,
		TC_START,
		TC_STOP,
		TC_KILL,
		TC_CONNECT,
		TC_DISCONNECT,
		TC_MAP,
		TC_UNMAP,
		TC_STOPPING,
		TC_EXITING,
		TC_EXITED,
		MTC_CONTROLPART,
		MTC_TESTCASE,
		MTC_ALL_COMPONENT_STOP,
		MTC_ALL_COMPONENT_KILL,
		MTC_TERMINATING_TESTCASE,
		MTC_PAUSED,
		PTC_FUNCTION,
		PTC_STARTING,
		PTC_STOPPED,
		PTC_KILLING,
		PTC_STOPPING_KILLING,
		PTC_STALE,
		TC_SYSTEM,
		MTC_CONFIGURING
	}

	/** Possible reasons for waking up the MC thread from the main thread. */
	private static enum wakeup_reason_t {
		REASON_NOTHING, REASON_SHUTDOWN, REASON_MTC_KILL_TIMER
	}

	/** Data structure for representing a port connection */
	private static class PortConnection {
		conn_state_enum conn_state;
		transport_type_enum transport_type;
		int headComp;
		String headPort;
		int tailComp;
		String tailPort;
		RequestorStruct requestors;
	}

	private static class unknown_connection {
		public SocketChannel channel;
		InetAddress ip_address;
		public Text_Buf text_buf;
	}

	/** Data structure for describing the component location constraints */
	private static class HostGroupStruct {
		String group_name;
		boolean has_all_hosts;
		boolean has_all_components;
		List<String> host_members;
		List<String> assigned_components;
	}

	/** Data structure for each host (and the corresponding HC) */
	public static class Host {
		public InetAddress ip_address;
		public String hostname;
		public String hostname_local;
		String machine_type;
		public String system_name;
		public String system_release;
		public String system_version;
		boolean transport_supported[];
		String log_source;
		public hc_state_enum hc_state;
		SocketChannel socket;// hc_fd
		public Text_Buf text_buf;
		public List<Integer> components;

		/* to implement load balancing mechanisms */
		public List<String> allowed_components;
		public boolean all_components_allowed;
		public int n_active_components;

		Host() {
			transport_supported = new boolean[transport_type_enum.TRANSPORT_NUM.ordinal()];
			components = new ArrayList<Integer>();
			allowed_components = new ArrayList<String>();
		}
	}

	public static class QualifiedName {
		public String module_name;
		public String definition_name;

		public QualifiedName(final String module_name, final String definition_name) {
			this.module_name = module_name;
			this.definition_name = definition_name;
		}

	}

	/** Data structure for each TC */
	public static class ComponentStruct {
		public int comp_ref;
		public QualifiedName comp_type;
		public String comp_name;
		String log_source;
		Host comp_location;
		public tc_state_enum tc_state;
		public VerdictTypeEnum local_verdict;
		String verdict_reason;
		SocketChannel socket;// tc_fd
		Text_Buf text_buf;
		public QualifiedName tc_fn_name;
		String return_type;
		byte[] return_value;
		boolean is_alive;
		boolean stop_requested;
		boolean process_killed;
		// initial
		ComponentStruct create_requestor;
		String location_str;
		// starting
		ComponentStruct start_requestor;
		// int arg;
		byte[] arg;
		RequestorStruct cancel_done_sent_to;
		// stopping_killing
		RequestorStruct stop_requestors;
		RequestorStruct kill_requestors;

		// On the C side these are lists linked through the PortConnections
		// Here the list is outside
		List<PortConnection> conn_head_list;
		List<PortConnection> conn_tail_list;

		RequestorStruct done_requestors;
		RequestorStruct killed_requestors;
		RequestorStruct cancel_done_sent_for;

		timer_struct kill_timer;
	}

	/** Structure for timers */
	private static class TimerStruct {
		double expiration;
		ComponentStruct component;
	}

	/**
	 * Container of test components (when a pending operation can be
	 * requested by several components)
	 */
	private static class RequestorStruct {
		List<ComponentStruct> components = new ArrayList<MainController.ComponentStruct>();
	}

	private UserInterface ui;
	private final NetworkHandler nh = new NetworkHandler();

	private mcStateEnum mc_state;
	private String mc_hostname;

	/**
	 * Use ServerSocketChannel and Selector for non-blocking I/O instead of
	 * file descriptor
	 */
	private ServerSocketChannel mc_channel;
	private Selector mc_selector;

	private enum channel_type_enum {
		CHANNEL_UNUSED, CHANNEL_SERVER, CHANNEL_UNKNOWN, CHANNEL_HC, CHANNEL_TC
	}

	private static class channel_table_struct {
		public channel_type_enum channel_type;

		public unknown_connection unknown;
		public Host host;
		public ComponentStruct component;
		// FIXME dummy?
	}

	private final ConcurrentHashMap<SelectableChannel, channel_table_struct> channel_table = new ConcurrentHashMap<SelectableChannel, MainController.channel_table_struct>();

	private static class module_version_info {
		public String module_name;
		public byte[] module_checksum;
	}

	private String config_str;

	// version_known is not need. if the modules list is not empty, we know the module version informations from the first HC to connect.
	private final List<module_version_info> modules = new ArrayList<MainController.module_version_info>();

	private volatile boolean any_component_done_requested;
	private volatile boolean any_component_done_sent;
	private volatile boolean all_component_done_requested;
	private volatile boolean any_component_killed_requested;
	private volatile boolean all_component_killed_requested;
	private long testcase_start_time_seconds;// testcase_start_time
	private int testcase_start_time_microseconds;
	private volatile boolean stop_requested;
	private volatile boolean stop_after_tc;

	private int next_comp_ref;
	private int tc_first_comp_ref;
	//can not be null on the Java side
	private final List<Host> hosts = new ArrayList<MainController.Host>();
	//can not be null on the Java side
	private final List<HostGroupStruct> host_groups = new ArrayList<HostGroupStruct>();
	private List<String> assigned_components = new ArrayList<String>();
	private volatile boolean all_components_assigned;
	private final Deque<unknown_connection> unknown_connections = new LinkedList<MainController.unknown_connection>();

	private int n_active_ptcs = 0;
	//a negative value means no check
	private int max_ptcs = -1;
	public List<ComponentStruct> components = new ArrayList<MainController.ComponentStruct>();

	private double kill_timer = 0.0;
	private final ReentrantLock mutex = new ReentrantLock();

	private ComponentStruct mtc;
	private ComponentStruct system;

	private wakeup_reason_t wakeup_reason;

	//TODO maybe could be turned into constructor
	public void initialize(final UserInterface par_ui, final int par_max_ptcs) {
		ui = par_ui;

		max_ptcs = par_max_ptcs;

		mc_state = mcStateEnum.MC_INACTIVE;

		try {
			mc_hostname = InetAddress.getLocalHost().getHostName();
		} catch (UnknownHostException e) {
			throw new TtcnError(e);
		}
		mc_hostname = String.format("MC@%s", mc_hostname);

		all_components_assigned = false;

		config_str = null;

		// component initialized with default initializer
		mtc = null;
		system = null;
		// debugger_active_tc = NULL;
		next_comp_ref = TitanComponent.FIRST_PTC_COMPREF;

		stop_after_tc = false;
		stop_requested = false;

		kill_timer = 10.0;

		wakeup_reason = wakeup_reason_t.REASON_NOTHING;
	}

	public void terminate() {
		clean_up();
		destroy_host_groups();
		// FIXME implement
	}

	private void lock() {
		// TODO handle error
		mutex.lock();
	}

	private void unlock() {
		// TODO handle error
		mutex.unlock();
	}

	public static class timer_struct {
		double expiration;
		// dummy?
		ComponentStruct component;
	}

	private final LinkedList<timer_struct> timers = new LinkedList<MainController.timer_struct>();

	private static double time_now() {
		// TODO check
		return System.currentTimeMillis() / 1000.0;
	}

	private void register_timer(final timer_struct timer) {
		if (timers.isEmpty()) {
			timers.add(timer);
			return;
		}

		final ListIterator<timer_struct> iterator = timers.listIterator();
		while (iterator.hasNext()) {
			final timer_struct listItem = iterator.next();
			if (listItem.expiration > timer.expiration) {
				iterator.previous();
				iterator.add(timer);
				return;
			}
		}

		timers.add(timer);
	}

	private void cancel_timer(final timer_struct timer) {
		timers.remove(timer);
	}

	private int get_poll_timeout() {
		final timer_struct timer_head = timers.peek();
		if (timer_head == null) {
			return -1;
		} else {
			final double offset = timer_head.expiration - time_now();
			if (offset > 0.0) {
				return (int) (1000.0 * offset);
			} else {
				return 0;
			}
		}
	}

	private void handle_expired_timers() {
		if (timers.isEmpty()) {
			return;
		}

		final double now = time_now();
		timer_struct head = timers.peek();
		while (head != null && head.expiration < now) {
			handle_kill_timer(head);
			head = timers.peek();
		}
	}

	private void start_kill_timer(final ComponentStruct tc) {
		if (kill_timer > 0.0) {
			final timer_struct timer = new timer_struct();
			timer.expiration = time_now() + kill_timer;
			timer.component = tc;
			tc.kill_timer = timer;
			register_timer(timer);
		} else {
			tc.kill_timer = null;
		}
	}

	private void init_connections(final ComponentStruct tc) {
		tc.conn_head_list = new ArrayList<MainController.PortConnection>();
		tc.conn_tail_list = new ArrayList<MainController.PortConnection>();
	}

	private void handle_kill_timer(final timer_struct timer) {
		final ComponentStruct tc = timer.component;
		final Host host = tc.comp_location;
		boolean kill_process = false;
		switch (tc.tc_state) {
		case TC_EXITED:
			// do nothing
			break;
		case TC_EXITING:
			if (tc == mtc) {
				error(MessageFormat.format("MTC on host {0} did not close its control connection in time. Trying to kill it using its HC.",
						host.hostname));
			} else {
				notify(MessageFormat.format("PTC {0} on host {1} did not close its control connection in time. Trying to kill it using its HC.",
						tc.comp_ref, host.hostname));
			}
			kill_process = true;
			break;
		case TC_STOPPING:
		case PTC_STOPPING_KILLING:
		case PTC_KILLING:
			// active PTCs with kill timer can be only in these states
			if (tc != mtc) {
				notify(MessageFormat.format("PTC {0} on host {1} is not responding. Trying to kill it using its HC.",
						tc.comp_ref, host.hostname));
				kill_process = true;
				break;
			}
			// no break;
		default:
			// MTC can be in any state
			if (tc == mtc) {
				error(MessageFormat.format("MTC on host {0} is not responding. Trying to kill it using its HC. This will abort test execution.",
						host.hostname));
				kill_process = true;
			} else {
				error(MessageFormat.format("PTC {0} is in invalid state when its kill timer expired.", tc.comp_ref));
			}
		}

		if (kill_process) {
			if (host.hc_state == hc_state_enum.HC_ACTIVE) {
				send_kill_process(host, tc.comp_ref);
				tc.process_killed = true;
			} else {
				error(MessageFormat.format("Test Component {0} cannot be killed because the HC on host {1} is not in active state. Kill the process manually or the test system may get into a deadlock.",
						tc.comp_ref, host.hostname));
			}
		}

		cancel_timer(timer);
		tc.kill_timer = null;
	}

	private void error(final String message) {
		unlock();
		ui.error(0, message);
		lock();
	}

	private void notify(final String message) {
		final long timestamp = System.currentTimeMillis();
		notify(new Timeval(timestamp / 1000, timestamp % 1000), mc_hostname, TTCN_Logger.Severity.EXECUTOR_UNQUALIFIED.ordinal(), message);
	}

	private void notify(final Timeval timestamp, final String source, final int severity, final String message) {
		unlock();
		ui.notify(timestamp, source, severity, message);
		lock();
	}

	private void status_change() {
		unlock();
		ui.status_change();
		lock();
	}

	private void fatal_error(final String message) {
		// FIXME implement fatal_error
	}

	private void add_component(final ComponentStruct component) {
		final int component_reference = component.comp_ref;
		if (lookup_component(component_reference) != null) {
			fatal_error(MessageFormat.format("MainController.add_component: duplicate component reference {0}.", component_reference));
		}

		while (components.size() < component_reference) {
			components.add(null);
		}

		components.add(component_reference, component);
	}

	private ComponentStruct lookup_component(final int component_reference) {
		if (component_reference > 0 && component_reference < components.size()) {
			return components.get(component_reference);
		} else {
			return null;
		}
	}

	private void destroy_all_components() {
		for (final ComponentStruct component : components) {
			if (component == null) {
				continue;
			}

			close_tc_connection(component);
			remove_component_from_host(component);
			component.comp_type = new QualifiedName("", "");
			component.tc_fn_name = new QualifiedName("", "");
			component.comp_name = null;
			component.return_type = null;
			component.return_value = null;
			component.verdict_reason = null;

			switch (component.tc_state) {
			case TC_INITIAL:
				component.location_str = null;
				break;
			case PTC_STARTING:
				free_requestors(component.cancel_done_sent_to);
				break;
			case TC_STOPPING:
			case PTC_STOPPING_KILLING:
			case PTC_KILLING:
				free_requestors(component.stop_requestors);
				free_requestors(component.kill_requestors);
				break;
			default:
				break;
			}

			free_requestors(component.done_requestors);
			free_requestors(component.killed_requestors);
			free_requestors(component.cancel_done_sent_for);
			remove_all_connections(component.comp_ref);
		}

		components.clear();
		n_active_ptcs = 0;
		mtc = null;
		system = null;

		for (final Host host : hosts) {
			host.n_active_components = 0;
		}

		next_comp_ref = TitanComponent.FIRST_PTC_COMPREF;

		any_component_done_requested = false;
		any_component_done_sent = false;
		all_component_done_requested = false;
		any_component_killed_requested = false;
		all_component_killed_requested = false;
	}

	private unknown_connection new_unknown_connection() {
		final unknown_connection temp = new unknown_connection();
		unknown_connections.add(temp);

		return temp;
	}

	private void delete_unknown_connection(final unknown_connection connection) {
		unknown_connections.remove(connection);
	}

	private void close_unknown_connection(final unknown_connection connection) {
		try {
			connection.channel.close();
		} catch (IOException e) {
			// FIXME handle
		}

		channel_table.remove(connection.channel);
		delete_unknown_connection(connection);
	}

	private void thread_main() {
		lock();
		while (mc_state != mcStateEnum.MC_INACTIVE) {
			// FIXME implement
			unlock();
			try {
				final int timeout = get_poll_timeout();
				int selectReturn;
				if (timeout > 0) {
					selectReturn = mc_selector.select(timeout);
				} else if (timeout < 0) {
					selectReturn = mc_selector.select(0);
				} else {
					selectReturn = mc_selector.selectNow();
				}
				lock();

				switch (wakeup_reason) {
				case REASON_NOTHING:
				case REASON_MTC_KILL_TIMER:
					break;
				case REASON_SHUTDOWN:
					wakeup_reason = wakeup_reason_t.REASON_NOTHING;
					perform_shutdown();
					continue;
				default:
					error(MessageFormat.format("Invalid wakeup reason ({0}) was set.", wakeup_reason));
					wakeup_reason = wakeup_reason_t.REASON_NOTHING;
					break;
				}

				if (selectReturn > 0) {
					final Set<SelectionKey> selectedKeys = mc_selector.selectedKeys();
					for (final SelectionKey key : selectedKeys) {
						final SelectableChannel keyChannel = key.channel();
						dispatch_socket_event(keyChannel);
					}
					selectedKeys.clear();
				} else if (selectReturn == 0) {
					handle_expired_timers();
				}

			} catch (IOException e) {
				/*if (local_address == null || local_address.isEmpty()) {
					error(MessageFormat.format("Listening on TCP port {0,number,#} failed: {1}\n", tcp_port, e.getMessage()));
					//clean up?
					return 0;
				} else {
					error(MessageFormat.format("Listening on IP address {0} and TCP port {1,number,#} failed: {2}\n", local_address, tcp_port, e.getMessage()));
					//clean up?
					return 0;
				}
				 */
				return;
			}
		}

		clean_up();
		notify("Shutdown complete.");
		unlock();
		// no locking
		ui.status_change();
	}

	private void dispatch_socket_event(final SelectableChannel channel) {
		if (!channel_table.containsKey(channel)) {
			return;
		}

		final channel_table_struct temp_struct = channel_table.get(channel);
		switch (temp_struct.channel_type) {
		case CHANNEL_SERVER:
			handle_incoming_connection((ServerSocketChannel) channel);
			break;
		case CHANNEL_UNKNOWN:
			handle_unknown_data(temp_struct.unknown);
			break;
		case CHANNEL_HC:
			handle_hc_data(temp_struct.host, true);
			break;
		case CHANNEL_TC:
			handle_tc_data(temp_struct.component, true);
			break;
		default:
			fatal_error(MessageFormat.format("Invalid connection type ({0}) for channel {1}.", temp_struct.channel_type, channel));
		}
	}

	private void wakeup_thread(final wakeup_reason_t reason) {
		//we don't need the pipe s we can wakeup the selector.
		wakeup_reason = reason;
		mc_selector.wakeup();
	}

	private void handle_incoming_connection(final ServerSocketChannel channel) {
		try {
			final SocketChannel sc = channel.accept();//FIXME can return null
			sc.configureBlocking(false);
			final unknown_connection new_connection = new_unknown_connection();
			new_connection.channel = sc;
			new_connection.ip_address = sc.socket().getInetAddress();
			new_connection.text_buf = new Text_Buf();

			final channel_table_struct new_struct = new channel_table_struct();
			new_struct.channel_type = channel_type_enum.CHANNEL_UNKNOWN;
			new_struct.unknown = new_connection;
			channel_table.put(sc, new_struct);
			try {
				sc.register(mc_selector, SelectionKey.OP_READ);
			} catch (ClosedChannelException e) {
				// FIXME different error
				error(MessageFormat.format("Selector registration failed: {0}.", e.getMessage()));
			}
		} catch (IOException e) {
			// FIXME handle error
		}
	}

	private void handle_unknown_data(final unknown_connection connection) {
		// Actually should come from the connection
		final Text_Buf text_buf = connection.text_buf;
		final int recv_len = receive_to_buffer(connection.channel, text_buf, true);
		boolean error_flag = false;

		if (recv_len > 0) {
			try {
				while (text_buf.is_message()) {
					final int msg_len = text_buf.pull_int().get_int();
					final int msg_type = text_buf.pull_int().get_int();
					boolean process_more_messages = false;
					switch (msg_type) {
					case MSG_ERROR:
						process_error(connection);
						process_more_messages = true;
						break;
					case MSG_LOG:
						process_log(connection);
						process_more_messages = true;
						break;
					case MSG_VERSION:
						process_version(connection);
						break;
					case MSG_MTC_CREATED:
						process_mtc_created(connection);
						break;
					case MSG_PTC_CREATED:
						process_ptc_created(connection);
						break;
					default:
						error(MessageFormat.format("Invalid message type ({0}) was received on an "
								+ "unknown connection from {1} [{2}].", 
								msg_type, connection.ip_address.getHostName(), connection.ip_address.getHostAddress()));
						error_flag = true;
					}

					if (process_more_messages) {
						text_buf.cut_message();
					} else {
						break;
					}
				}
			} catch (TtcnError tc_error) {
				error(MessageFormat.format("Maleformed message was received on an unknown connection from {0} [{1}].",
						connection.ip_address.getHostName(), connection.ip_address.getHostAddress()));
				error_flag = true;
			}
			if (error_flag) {
				send_error_str(connection.channel, "The received message was not understood by the MC.");
			}
		} else if (recv_len == 0) {
			error(MessageFormat.format("Unexpected end of an unknown connection from {0} [{1}].",
					connection.ip_address.getHostName(), connection.ip_address.getHostAddress()));
			error_flag = true;
		} else {
			error(MessageFormat.format("Receiving of data failed on an unknown connection from {0} [{1}].",
					connection.ip_address.getHostName(), connection.ip_address.getHostAddress()));
			error_flag = true;
		}
		if (error_flag) {
			close_unknown_connection(connection);
		}
	}

	public void add_host(final String group_name, final String host_name) {
		lock();
		if (mc_state != mcStateEnum.MC_INACTIVE) {
			error("MainController.add_host: called in wrong state.");
			unlock();
			return;
		}

		final HostGroupStruct group = add_host_group(group_name);
		if (host_name != null) {
			if (group.has_all_hosts) {
				error(MessageFormat.format("Redundant member `{0}' was ignored in host group `{1}'. All hosts (`*') are already the members of the group.",
						host_name, group_name));
			} else {
				if (group.host_members.contains(host_name)) {
					error(MessageFormat.format("Duplicate member `{0}' was ignored in host group `{1}'.",
							host_name, group_name));
				} else {
					group.host_members.add(host_name);
				}
			}
		} else {
			if (group.has_all_hosts) {
				error(MessageFormat.format("Duplicate member `*' was ignored in host group `{0}'.", group_name));
			} else {
				for (final String group_member : group.host_members) {
					error(MessageFormat.format("Redundant member `{0}' was ignored in host group `{1}'. All hosts (`*') are already the members of the group.",
							group_member, group_name));
				}

				group.host_members.clear();
				group.has_all_hosts = true;
			}
		}

		unlock();
	}

	private HostGroupStruct add_host_group(final String group_name) {
		for (final HostGroupStruct current_group : host_groups) {
			if (current_group.group_name.equals(group_name)) {
				return current_group;
			}
		}

		final HostGroupStruct new_group = new HostGroupStruct();
		new_group.group_name = group_name;
		new_group.has_all_hosts = false;
		new_group.has_all_components = false;
		new_group.host_members = new ArrayList<String>();
		new_group.assigned_components = new ArrayList<String>();

		host_groups.add(new_group);
		return new_group;
	}

	private HostGroupStruct lookup_host_group(final String group_name) {
		for (final HostGroupStruct group : host_groups){
			if (group.group_name.equals(group_name)) {
				return group;
			}
		}

		return null;
	}

	private static boolean is_similar_hostname(final String host1, final String host2) {
		final String host1Lower = host1.toLowerCase();
		final String host2lower = host2.toLowerCase();
		for (int i = 0; i <= Math.min(host1.length(), host2.length()); i++) {
			// case insensitive comparison of the characters
			if (host1Lower.charAt(i) != host2lower.charAt(i)) {
				return false;
			}
			// continue the evaluation if they are matching
		}

		if (host1.length() == host2.length()) {
			return true;
		}

		if (host1.length() < host2.length()) {
			// if host2 is the longer one it may contain an additional domain
			// name with a leading dot (e.g. "foo" is similar to "foo.bar.com")
			// note: empty string is similar with empty string only
			if (host2.charAt(host1.length()) == '.') {
				return true;
			} else {
				return false;
			}
		} else {
			// or vice versa
			if (host1.charAt(host2.length()) == '.') {
				return true;
			} else {
				return false;
			}
		}
	}

	private static boolean host_has_name(final Host host, final String name) {
		// name might resemble to host->hostname
		if (is_similar_hostname(host.hostname, name)) {
			return true;
		}
		// to avoid returning true in situations when name is "foo.bar.com", but
		// host->hostname is "foo.other.com" and host->hostname_local is "foo"
		// name might resemble to host->hostname_local
		//FIXME local_hostname_different
		if (is_similar_hostname(host.hostname_local, name)) {
			return true;
		}

		//FIXME add support for IP address
		return false;
	}

	private static boolean member_of_group(final Host host, final HostGroupStruct group) {
		if (group.has_all_hosts) {
			return true;
		}

		if (group.host_members.isEmpty()) {
			return host_has_name(host, group.group_name);
		}

		for (int i = 0; i < group.host_members.size(); i++) {
			final String member_name = group.host_members.get(i);
			if (host_has_name(host, member_name)) {
				return true;
			}
		}

		return false;
	}

	private void add_allowed_components(final Host host) {
		host.all_components_allowed = false;
		for (final HostGroupStruct group: host_groups) {
			if (member_of_group(host, group)) {
				continue;
			}

			for (int j = 0; j < group.assigned_components.size();j++) {
				final String component_id = group.assigned_components.get(j);
				if (component_id == null) {
					break;
				}

				host.allowed_components.add(component_id);
			}

			if (group.has_all_components) {
				host.all_components_allowed = true;
			}
		}
	}

	public void assign_component(final String host_or_group, final String component_id) {
		lock();
		if (mc_state != mcStateEnum.MC_INACTIVE) {
			error("MainController.assign_component: called in wrong state.");
			unlock();
			return;
		}

		final HostGroupStruct group = add_host_group(host_or_group);
		if (component_id == null) {
			if (all_components_assigned) {
				for (final HostGroupStruct hostGroup : host_groups) {
					if (hostGroup.has_all_components) {
						error(MessageFormat.format("Duplicate assignment of all components (*) to host group `{0}'. Previous assignment to group `{1}' is ignored.",
								host_or_group, hostGroup.group_name));
						hostGroup.has_all_components = false;
					}
				}
			} else {
				all_components_assigned = true;
			}

			group.has_all_components = true;
		} else {
			if (assigned_components.contains(component_id)) {
				for (final HostGroupStruct hostGroup : host_groups) {
					if (hostGroup.assigned_components.contains(component_id)) {
						error(MessageFormat.format("Duplicate assignment of component `{0}' to host group `{1}'. Previous assignment to group `{2}' is ignored.",
								component_id, host_or_group, hostGroup.group_name));
						hostGroup.assigned_components.remove(component_id);
					}
				}
			} else {
				assigned_components.add(component_id);
			}

			group.assigned_components.add(component_id);
		}

		unlock();
	}

	public void destroy_host_groups() {
		lock();
		if (mc_state != mcStateEnum.MC_INACTIVE) {
			error("MainController.destroy_host_groups: called in wrong state.");
		} else {
			for (final HostGroupStruct hostGroup : host_groups) {
				hostGroup.host_members.clear();
				hostGroup.assigned_components.clear();
			}

			host_groups.clear();
			assigned_components.clear();
			all_components_assigned = false;
		}

		unlock();
	}

	public int start_session(final String local_address, int tcp_port) {
		lock();
		try {
			if (mc_state != mcStateEnum.MC_INACTIVE) {
				error("MainController.start_session: called in wrong state.");
				return 0;
			}

			try {
				mc_selector = Selector.open();
			} catch (IOException e) {
				error(MessageFormat.format("Selector opening failed: {0}.", e.getMessage()));
				clean_up();
				unlock();
				return 0;
			}

			nh.set_family(local_address, tcp_port);

			try {
				mc_channel = ServerSocketChannel.open();
				mc_channel.configureBlocking(false);
			} catch (IOException e) {
				error(MessageFormat.format("Server socket creation failed: {0}\n", e.getMessage()));
				clean_up();
				return 0;
			}

			try {
				mc_channel.setOption(StandardSocketOptions.SO_REUSEADDR, true);
			} catch (IOException e) {
				error(MessageFormat.format("SO_REUSEADDR failed on server socket: {0}", e.getMessage()));
				clean_up();
				return 0;
			}

			// TCP_NODELAY not supported for server sockets in Java

			try {
				mc_channel.bind(nh.get_addr(), 10);
			} catch (IOException e) {
				if (local_address == null || local_address.isEmpty()) {
					error(MessageFormat.format("Binding server socket to TCP port {0,number,#} failed: {1}\n", tcp_port, e.getMessage()));
					clean_up();
					return 0;
				} else {
					error(MessageFormat.format("Binding server socket to IP address {0} and TCP port {1,number,#} failed: {2}\n", local_address, tcp_port, e.getMessage()));
					clean_up();
					return 0;
				}
			}

			try {
				final channel_table_struct server_struct = new channel_table_struct();
				server_struct.channel_type = channel_type_enum.CHANNEL_SERVER;
				channel_table.put(mc_channel, server_struct);
				mc_channel.register(mc_selector, SelectionKey.OP_ACCEPT);
			} catch (ClosedChannelException e) {
				error(MessageFormat.format("Selector registration failed: {0}.", e.getMessage()));
				clean_up();
				unlock();
				return 0;
			}

			wakeup_reason = wakeup_reason_t.REASON_NOTHING;

			mc_state = mcStateEnum.MC_LISTENING;
			final Thread thread = new Thread() {

				@Override
				public void run() {
					thread_main();
				}

			};

			thread.start();

			try {
				tcp_port = ((InetSocketAddress) mc_channel.getLocalAddress()).getPort();
				notify(MessageFormat.format("Listening on IP address {0} and TCP port {1,number,#}.\n",
						((InetSocketAddress) mc_channel.getLocalAddress()).getAddress().getHostAddress(),
						((InetSocketAddress) mc_channel.getLocalAddress()).getPort()));
			} catch (IOException e) {
				// FIXME what can be the error here?
			}

			ui.status_change();

			return tcp_port;
		} finally {
			unlock();
		}

	}

	public void set_kill_timer(final Double timer_val) {
		lock();
		switch (mc_state) {
		case MC_INACTIVE:
		case MC_LISTENING:
		case MC_HC_CONNECTED:
		case MC_RECONFIGURING:
			if (timer_val < 0.0) {
				error("MainController.set_kill_timer: setting a negative kill timer value.");
			} else {
				kill_timer = timer_val;
			}
			break;
		default:
			error("MainController.set_kill_timer: called in wrong state.");
			break;
		}

		unlock();
	}

	public mcStateEnum get_state() {
		lock();
		final mcStateEnum ret_val = mc_state;
		unlock();
		return ret_val;
	}

	public boolean get_stop_after_testcase() {
		lock();
		final boolean ret_val = stop_after_tc;
		unlock();
		return ret_val;
	}

	public int get_nof_hosts() {
		lock();
		final int ret_val = hosts.size();
		unlock();
		return ret_val;
	}

	public Host get_host_data(final int host_index) {
		lock();
		return hosts.get(host_index);
	}

	public ComponentStruct get_component_data(final int component_reference) {
		lock();
		return lookup_component(component_reference);
	}

	public void release_data() {
		unlock();
	}

	public static String get_mc_state_name(final mcStateEnum state) {
		switch (state) {
		case MC_INACTIVE:
			return "inactive";
		case MC_LISTENING:
			return "listening";
		case MC_LISTENING_CONFIGURED:
			return "listening (configured)";
		case MC_HC_CONNECTED:
			return "HC connected";
		case MC_CONFIGURING:
			return "configuring...";
		case MC_ACTIVE:
			return "active";
		case MC_CREATING_MTC:
			return "creating MTC...";
		case MC_TERMINATING_MTC:
			return "terminating MTC...";
		case MC_READY:
			return "ready";
		case MC_EXECUTING_CONTROL:
			return "executing control part";
		case MC_EXECUTING_TESTCASE:
			return "executing testcase";
		case MC_TERMINATING_TESTCASE:
			return "terminating testcase...";
		case MC_PAUSED:
			return "paused after testcase";
		case MC_SHUTDOWN:
			return "shutting down...";
		default:
			return "unknown/transient";
		}
	}

	public static String get_hc_state_name(final hc_state_enum state) {
		switch (state) {
		case HC_IDLE:
			return "not configured";
		case HC_CONFIGURING:
		case HC_CONFIGURING_OVERLOADED:
			return "being configured";
		case HC_ACTIVE:
			return "ready";
		case HC_OVERLOADED:
			return "overloaded";
		case HC_DOWN:
			return "down";
		default:
			return "unknown/transient";
		}
	}

	public static String get_tc_state_name(final tc_state_enum state) {
		switch (state) {
		case TC_INITIAL:
			return "being created";
		case TC_IDLE:
			return "inactive - waiting for start";
		case TC_CREATE:
			return "executing create operation";
		case TC_START:
			return "executing component start operation";
		case TC_STOP:
		case MTC_ALL_COMPONENT_STOP:
			return "executing component stop operation";
		case TC_KILL:
		case MTC_ALL_COMPONENT_KILL:
			return "executing kill operation";
		case TC_CONNECT:
			return "executing connect operation";
		case TC_DISCONNECT:
			return "executing disconnect operation";
		case TC_MAP:
			return "executing map operation";
		case TC_UNMAP:
			return "executing unmap operation";
		case TC_STOPPING:
			return "being stopped";
		case TC_EXITING:
			return "terminated";
		case TC_EXITED:
			return "exited";
		case MTC_CONTROLPART:
			return "executing control part";
		case MTC_TESTCASE:
			return "executing testcase";
		case MTC_TERMINATING_TESTCASE:
			return "terminating testcase";
		case MTC_PAUSED:
			return "paused";
		case PTC_FUNCTION:
			return "executing function";
		case PTC_STARTING:
			return "being started";
		case PTC_STOPPED:
			return "stopped - waiting for re-start";
		case PTC_KILLING:
		case PTC_STOPPING_KILLING:
			return "being killed";
		default:
			return "unknown/transient";
		}
	}

	// Temporary
	public List<Host> get_hosts() {
		return hosts;
	}
	
	public synchronized boolean start_reconfiguring() {
		switch (mc_state) {
		case MC_READY:
			mc_state = mcStateEnum.MC_RECONFIGURING;
			return true;
		case MC_LISTENING:
		case MC_HC_CONNECTED:
			return true;
		default:
			lock();
			error("MainController.start_reconfiguring: called in wrong state.");
			unlock();
			return false;
		}
	}

	public synchronized void create_mtc(final Host host) {
		lock();
		if (mc_state != mcStateEnum.MC_ACTIVE) {
			error("MainController.create_mtc: called in wrong state.");
			unlock();
			return;
		}

		switch (host.hc_state) {
		case HC_OVERLOADED:
			notify(MessageFormat.format("HC on host {0} reported overload. Trying to create MTC there anyway.", host.hostname));
		case HC_ACTIVE:
			break;
		default:
			error(MessageFormat.format("MTC cannot be created on {0}: HC is not active.", host.hostname));
			unlock();
			return;
		}

		notify(MessageFormat.format("Creating MTC on host {0}.", host.hostname));
		send_create_mtc(host);

		mtc = new ComponentStruct();
		mtc.comp_ref = TitanComponent.MTC_COMPREF;
		mtc.comp_name = "MTC";
		mtc.tc_state = tc_state_enum.TC_INITIAL;
		mtc.local_verdict = VerdictTypeEnum.NONE;
		mtc.verdict_reason = null;
		mtc.text_buf = null;
		mtc.return_type = null;
		mtc.return_value = null;
		mtc.is_alive = false;
		mtc.stop_requested = false;
		mtc.process_killed = false;
		mtc.create_requestor = null;
		mtc.location_str = null;
		mtc.done_requestors = init_requestors(null);
		mtc.killed_requestors = init_requestors(null);
		mtc.cancel_done_sent_for = init_requestors(null);
		mtc.kill_timer = null;
		init_connections(mtc);
		add_component(mtc);
		add_component_to_host(host, mtc);
		host.n_active_components++;

		system = new ComponentStruct();
		system.comp_ref = TitanComponent.SYSTEM_COMPREF;
		system.comp_name = "SYSTEM";
		system.log_source = null;
		system.comp_location = null;
		system.tc_state = tc_state_enum.TC_SYSTEM;
		system.local_verdict = VerdictTypeEnum.NONE;
		system.verdict_reason = null;
		system.text_buf = null;
		system.return_type = null;
		system.return_value = null;
		system.is_alive = false;
		system.stop_requested = false;
		system.process_killed = false;
		system.done_requestors = init_requestors(null);
		system.killed_requestors = init_requestors(null);
		system.cancel_done_sent_for = init_requestors(null);
		system.kill_timer = null;
		init_connections(system);
		add_component(system);

		mc_state = mcStateEnum.MC_CREATING_MTC;
		status_change();
		unlock();
	}

	private void process_error(final unknown_connection connection) {
		final Text_Buf text_buf = connection.text_buf;
		final String reason = text_buf.pull_string();
		error(MessageFormat.format("Error message was received on an unknown connection from {0} [{1}]: {2}",
				connection.ip_address.getHostName(), connection.ip_address.getHostAddress(), reason));

		text_buf.cut_message();
		status_change();
	}

	private void process_error(final ComponentStruct tc) {
		final Text_Buf text_buf = tc.text_buf;
		final String reason = text_buf.pull_string();

		if (tc.equals(mtc)) {
			error(MessageFormat.format("Error message was received from the MTC at {0} [{1}]: {2}",
					mtc.comp_location.hostname, mtc.comp_location.ip_address.getHostAddress(), reason));
		} else {
			notify(MessageFormat.format("Error message was received from PTC {0} at {1} [{2}]: {3}",
					tc.comp_ref, tc.comp_location.hostname, tc.comp_location.ip_address.getHostAddress(), reason));
		}
	}

	private void process_error(final Host hc) {
		final Text_Buf text_buf = hc.text_buf;
		final String reason = text_buf.pull_string();
		error(MessageFormat.format("Error message was received from HC at {0} [{1}]: {2}",
				hc.hostname, hc.ip_address.getHostAddress(), reason));
	}

	private void send_create_mtc(final Host host) {
		final Text_Buf text_buf = new Text_Buf();
		text_buf.push_int(MSG_CREATE_MTC);
		send_message(host.socket, text_buf);
	}

	private void handle_hc_data(final Host hc, final boolean receive_from_socket) {
		final Text_Buf text_buf = hc.text_buf;
		boolean error_flag = false;
		final int recv_len = receive_to_buffer(hc.socket, text_buf, receive_from_socket);

		if (recv_len >= 0) {
			try {
				while (text_buf.is_message()) {
					final int msg_len = text_buf.pull_int().get_int();
					final int msg_type = text_buf.pull_int().get_int();
					switch (msg_type) {
					case MSG_ERROR:
						process_error(hc);
						break;
					case MSG_LOG:
						process_log(hc);
						break;
					case MSG_CONFIGURE_ACK:
						process_configure_ack(hc);
						break;
					case MSG_CONFIGURE_NAK:
						process_configure_nak(hc);
						break;
					case MSG_CREATE_NAK:
						process_create_nak(hc);
						break;
					case MSG_HC_READY:
						process_hc_ready(hc);
						break;
					case MSG_DEBUG_RETURN_VALUE:
						//FIXME: process_debug_return_value(*hc->text_buf, hc->log_source, msg_end, false);
						break;
					default:
						error(MessageFormat.format("Invalid message type ({0}) was received on HC connection from {1} [{2}].",
								msg_type, hc.hostname, hc.ip_address.getHostAddress()));
						error_flag = true;
					}
					if (error_flag) {
						break;
					}
					text_buf.cut_message();
				}
			} catch (TtcnError tc_error) {
				error(MessageFormat.format("Malformed message was received on HC connection from {0} [{1}].",
						hc.hostname, hc.ip_address.getHostAddress()));
				error_flag = true;
			}
			if (error_flag) {
				send_error_str(hc.socket, "The received message was not understood by the MC.");
			}
		} else if (recv_len == -1) {
			if (hc.hc_state == hc_state_enum.HC_EXITING) {
				close_hc_connection(hc);
				if (mc_state == mcStateEnum.MC_SHUTDOWN && all_hc_in_state(hc_state_enum.HC_DOWN)) {
					mc_state = mcStateEnum.MC_INACTIVE;
				} else {
					error(MessageFormat.format("Unexpected end of HC connection from {0} [{1}].",
							hc.hostname, hc.ip_address.getHostAddress()));
					error_flag = true;
				}
			}
		} else {
			error(MessageFormat.format("Receiving of data failed on HC connection from {0} [{1}].",
					hc.hostname, hc.ip_address.getHostAddress()));
			error_flag = true;
		}

		if (error_flag) {
			close_hc_connection(hc);
			switch (mc_state) {
			case MC_INACTIVE:
			case MC_LISTENING:
			case MC_LISTENING_CONFIGURED:
				fatal_error("MC is in invalid state when a HC connection terminated.");
				break;
			case MC_HC_CONNECTED:
				if (all_hc_in_state(hc_state_enum.HC_DOWN)) {
					mc_state = mcStateEnum.MC_LISTENING;
				}
				break;
			case MC_CONFIGURING:
			case MC_RECONFIGURING:
				check_all_hc_configured();
				break;
			case MC_ACTIVE:
				if (all_hc_in_state(hc_state_enum.HC_DOWN)) {
					mc_state = mcStateEnum.MC_LISTENING_CONFIGURED;
				} else if (!is_hc_in_state(hc_state_enum.HC_ACTIVE) && !is_hc_in_state(hc_state_enum.HC_OVERLOADED)) {
					mc_state = mcStateEnum.MC_HC_CONNECTED;
				}
				break;
			default:
				if (!is_hc_in_state(hc_state_enum.HC_ACTIVE)) {
					notify("There is no active HC connection. Further create operations will fail.");
				}
			}

			status_change();
		}
	}

	private void process_configure_nak(final Host hc) {
		switch (hc.hc_state) {
		case HC_CONFIGURING:
		case HC_CONFIGURING_OVERLOADED:
			hc.hc_state = hc_state_enum.HC_IDLE;
			break;
		default:
			send_error_str(hc.socket, "Unexpected message CONFIGURE_NAK was received.");
			return;
		}

		if (mc_state == mcStateEnum.MC_CONFIGURING || mc_state == mcStateEnum.MC_RECONFIGURING) {
			check_all_hc_configured();
		} else {
			notify(MessageFormat.format("Processing of configuration file failed on host {0}.", hc.hostname));
		}

		status_change();
	}

	private void process_create_nak(final Host hc) {
		switch (mc_state) {
		case MC_CREATING_MTC:
		case MC_EXECUTING_TESTCASE:
		case MC_TERMINATING_TESTCASE:
			break;
		default:
			send_error(hc.socket, "Message CREATE_NAK arrived in invalid state.");
			return;
		}

		switch (hc.hc_state) {
		case HC_ACTIVE:
			notify(MessageFormat.format("Host {0} is overloaded. New components will not be created there until further notice.",
					hc.hostname));
			// no break
		case HC_OVERLOADED:
			break;
		default:
			send_error(hc.socket, "Unexpected message CREATE_NAK was received: the sender is in invalid state.");
			return;
		}

		final Text_Buf text_buf = hc.text_buf;
		final int component_reference = text_buf.pull_int().get_int();

		switch (component_reference) {
		case TitanComponent.NULL_COMPREF:
			send_error(hc.socket, "Message CREATE_NAK refers to the null component reference.");
			return;
		case TitanComponent.SYSTEM_COMPREF:
			send_error(hc.socket, "Message CREATE_NAK refers to the component reference of the system.");
			return;
		case TitanComponent.ANY_COMPREF:
			send_error(hc.socket, "Message CREATE_NAK refers to 'any component'.");
			return;
		case TitanComponent.ALL_COMPREF:
			send_error(hc.socket, "Message CREATE_NAK refers to 'all component'.");
			return;
		}

		final ComponentStruct tc = lookup_component(component_reference);
		if (tc == null) {
			send_error(hc.socket, MessageFormat.format("Message CREATE_NAK refers to invalid component reference {0}.",
					component_reference));
			return;
		}
		if (tc.tc_state != tc_state_enum.TC_INITIAL) {
			send_error(hc.socket, MessageFormat.format("Message CREATE_NAK refers to test component {0}, which is not being created.",
					component_reference));
			return;
		}
		if (tc.comp_location != hc) {
			send_error(hc.socket, MessageFormat.format("Message CREATE_NAK refers to test component {0}, which was assigned to a different host ({1}).",
					component_reference, tc.comp_location.hostname));
			return;
		}

		remove_component_from_host(tc);
		hc.n_active_components--;

		final String reason = text_buf.pull_string();
		if (tc == mtc) {
			if (mc_state != mcStateEnum.MC_CREATING_MTC) {
				fatal_error("MainController::process_create_nak: MC is in unexpected state when CREATE_NAK refers to MTC.");
			}

			error(MessageFormat.format("Creation of MTC failed on host {0}: {1}.",
					hc.hostname, reason));
			destroy_all_components();
			mc_state = mcStateEnum.MC_ACTIVE;
		} else {
			Host new_host = choose_ptc_location(tc.comp_type.definition_name, tc.comp_name, tc.location_str);
			if (new_host != null) {
				send_create_ptc(new_host, component_reference, tc.comp_type.module_name, tc.comp_type.definition_name, system.comp_type, tc.comp_name, tc.is_alive ? 1 : 0, mtc.tc_fn_name);
				notify(MessageFormat.format("PTC with component reference {0} was relocated from host {1} to {2} because of overload: {3}.",
						component_reference, hc.hostname, new_host.hostname, reason));
				add_component_to_host(new_host, tc);
				new_host.n_active_components++;
			} else {
				String component_data = MessageFormat.format("component type: {0}.{1}",
						tc.comp_type.module_name, tc.comp_type.definition_name);
				if (tc.comp_name != null) {
					component_data += MessageFormat.format(", name: {0}", tc.comp_name);
				}
				if (tc.location_str != null && tc.location_str.startsWith("\0")) {
					component_data += MessageFormat.format(", location: {0}", tc.location_str);
				}

				final ComponentStruct create_requestor = tc.create_requestor;
				if (create_requestor.tc_state == tc_state_enum.TC_CREATE) {
					send_error(tc.socket, MessageFormat.format("Creation of the new PTC ({0}) failed on host {1}: {2}. Other suitable hosts to relocate the component are not available.",
							component_data, hc.hostname, reason));
					if (create_requestor == mtc) {
						create_requestor.tc_state = tc_state_enum.MTC_TESTCASE;
					} else {
						create_requestor.tc_state = tc_state_enum.PTC_FUNCTION;
					}
				}

				tc.location_str = null;
				tc.tc_state = tc_state_enum.PTC_STALE;
				n_active_ptcs--;
				switch (mtc.tc_state) {
				case MTC_TERMINATING_TESTCASE:
					if (ready_to_finish_testcase()) {
						finish_testcase();
					}
					break;
				case MTC_ALL_COMPONENT_KILL:
					check_all_component_kill();
					break;
				case MTC_ALL_COMPONENT_STOP:
					check_all_component_stop();
					break;
				default:
					break;
				}

				notify(MessageFormat.format("Creation of a PTC ({0}) failed on host {1}: {2}. Relocation to other suitable host is not possible.",
						component_data, hc.hostname, reason));
			}
		}

		status_change();
	}

	private void process_hc_ready(final Host hc) {
		switch (hc.hc_state) {
		case HC_OVERLOADED:
			hc.hc_state = hc_state_enum.HC_ACTIVE;
			break;
		case HC_CONFIGURING_OVERLOADED:
			hc.hc_state = hc_state_enum.HC_CONFIGURING;
			break;
		default:
			send_error(hc.socket, "Unexpected message HC_READY was received.");
			return;
		}

		notify(MessageFormat.format("Host {0} is no more overloaded.", hc.hostname));
		status_change();
	}

	private void send_error(final SocketChannel channel, final String reason) {
		final Text_Buf text_buf = new Text_Buf();
		text_buf.push_int(MSG_ERROR);
		text_buf.push_string(reason);
		send_message(channel, text_buf);
	}

	// the same as send_error_str on the Java side
	private void send_error_str(final SocketChannel channel, final String reason) {
		final Text_Buf text_buf = new Text_Buf();
		text_buf.push_int(MSG_ERROR);
		text_buf.push_string(reason);
		send_message(channel, text_buf);
	}

	private void process_create_req(final ComponentStruct tc) {
		if (!request_allowed(mtc, "CREATE_REQ")) {
			return;
		}

		if (max_ptcs >= 0 && n_active_ptcs >= max_ptcs) {
			send_error(tc.socket, MessageFormat.format("The license key does not allow more than {0} simultaneously active PTCs.", max_ptcs));
		}

		final Text_Buf text_buf = tc.text_buf;

		final String componentTypeModule = text_buf.pull_string();
		final String componentTypeName = text_buf.pull_string();
		final String componentName = text_buf.pull_string();
		final String componentLocation = text_buf.pull_string();
		final int isAlive = text_buf.pull_int().get_int();
		final int upper_int = text_buf.pull_int().get_int();
		final int lower_int = text_buf.pull_int().get_int();
		testcase_start_time_seconds = upper_int * 0xffffffff + lower_int;
		testcase_start_time_microseconds = text_buf.pull_int().get_int();

		final Host ptcLoc = choose_ptc_location(componentTypeName, componentName, componentLocation);
		if (ptcLoc == null) {
			if (!is_hc_in_state(hc_state_enum.HC_ACTIVE)) {
				send_error(tc.socket, "There is no active HC connection. Create operation cannot be performed.");
			} else {
				String compData = "component type: " + componentTypeModule + "." + componentTypeName;
				if (componentName != null) {
					compData = compData + ", name: " + componentName;
				}
				if (componentLocation != null) {
					compData = compData + ", location: " + componentLocation;
				}
				send_error(tc.socket, MessageFormat.format("No suitable host was found to create a new PTC ({0}).", compData));
			}

			return;
		}

		final int comp_ref = next_comp_ref++;
		send_create_ptc(ptcLoc, comp_ref, componentTypeModule, componentTypeName, system.comp_type,
				componentName, isAlive, mtc.tc_fn_name);

		tc.tc_state = tc_state_enum.TC_CREATE;

		final ComponentStruct new_ptc = new ComponentStruct();
		new_ptc.comp_ref = comp_ref;
		new_ptc.comp_type = new QualifiedName(componentTypeModule, componentTypeName);
		new_ptc.comp_name = componentName;
		new_ptc.comp_location = ptcLoc;
		new_ptc.tc_state = tc_state_enum.TC_INITIAL;
		new_ptc.local_verdict = VerdictTypeEnum.NONE;
		new_ptc.verdict_reason = null;
		new_ptc.socket = null;
		new_ptc.text_buf = null;
		new_ptc.tc_fn_name = new QualifiedName("", "");
		new_ptc.return_type = null;
		new_ptc.return_value = null;
		new_ptc.is_alive = (isAlive == 1);
		new_ptc.stop_requested = false;
		new_ptc.process_killed = false;
		new_ptc.create_requestor = tc;
		new_ptc.location_str = componentLocation;
		new_ptc.done_requestors = init_requestors(null);
		new_ptc.killed_requestors = init_requestors(null);
		new_ptc.cancel_done_sent_for = init_requestors(null);
		new_ptc.kill_timer = null;
		init_connections(new_ptc);

		add_component(new_ptc);
		add_component_to_host(ptcLoc, new_ptc);
		ptcLoc.n_active_components++;
		n_active_ptcs++;

		status_change();
	}

	private Host choose_ptc_location(final String componentTypeName, final String componentName, final String componentLocation) {
		Host best_candidate = null;
		int load_on_best_candidate = 0;
		final boolean has_constraint = assigned_components.contains(componentTypeName) || assigned_components.contains(componentName);
		HostGroupStruct group;
		if (componentLocation != null && !componentLocation.isEmpty()) {
			group = lookup_host_group(componentLocation);
		} else {
			group = null;
		}

		for (final Host host : hosts) {
			if (host.hc_state != hc_state_enum.HC_ACTIVE) {
				continue;
			}

			if (best_candidate != null && host.n_active_components >= load_on_best_candidate) {
				continue;
			}

			if (componentLocation != null && !componentLocation.isEmpty()) {
				// the explicit location has precedence over the constraints
				if (group != null) {
					if (!member_of_group(host, group)) {
						continue;
					}
				} else {
					if (!host_has_name(host, componentLocation)) {
						continue;
					}
				}
			} else if (has_constraint){
				if (!host.allowed_components.contains(componentTypeName) &&
						!host.allowed_components.contains(componentName)) {
					continue;
				}
			} else if (all_components_assigned) {
				if (!host.all_components_allowed) {
					continue;
				}
			}

			best_candidate = host;
			load_on_best_candidate = host.n_active_components;
		}

		return best_candidate;
	}

	private void send_create_ptc(final Host host, final int comp_ref, final String componentTypeModule, final String componentTypeName,
			final QualifiedName comp_type, final String componentName, final int isAlive, final QualifiedName tc_fn_name) {
		final Text_Buf text_buf = new Text_Buf();
		text_buf.push_int(MSG_CREATE_PTC);
		text_buf.push_int(comp_ref);
		text_buf.push_string(componentTypeModule);
		text_buf.push_string(componentTypeName);
		text_buf.push_string(comp_type.module_name);
		text_buf.push_string(comp_type.definition_name);
		text_buf.push_string(componentName);
		text_buf.push_int(isAlive);
		text_buf.push_string(mtc.tc_fn_name.module_name);
		text_buf.push_string(mtc.tc_fn_name.definition_name);
		final int upper_int = (int)(testcase_start_time_seconds / 0xffffffff);
		final int lower_int = (int)(testcase_start_time_seconds % 0xffffffff);
		text_buf.push_int(upper_int);
		text_buf.push_int(lower_int);
		text_buf.push_int(testcase_start_time_microseconds);

		send_message(host.socket, text_buf);
	}

	private void process_configure_ack(final Host hc) {
		switch (hc.hc_state) {
		case HC_CONFIGURING:
			hc.hc_state = hc_state_enum.HC_ACTIVE;
			break;
		case HC_CONFIGURING_OVERLOADED:
			hc.hc_state = hc_state_enum.HC_OVERLOADED;
			break;
		default:
			send_error_str(hc.socket, "Unexpected message CONFIGURE_ACK was received.");
		}

		if (mc_state == mcStateEnum.MC_CONFIGURING || mc_state == mcStateEnum.MC_RECONFIGURING) {
			check_all_hc_configured();
		} else {
			notify(MessageFormat.format("Host {0} was configured successfully.", hc.hostname));
		}

		status_change();
	}

	private void check_all_hc_configured() {
		final boolean reconf = (mc_state == mcStateEnum.MC_RECONFIGURING);
		if (is_hc_in_state(hc_state_enum.HC_CONFIGURING) ||
				is_hc_in_state(hc_state_enum.HC_CONFIGURING_OVERLOADED)) {
			return;
		}

		if (is_hc_in_state(hc_state_enum.HC_IDLE)) {
			error("There were errors during configuring HCs.");
			mc_state = reconf ? mcStateEnum.MC_READY : mcStateEnum.MC_HC_CONNECTED;
		} else if (is_hc_in_state(hc_state_enum.HC_ACTIVE) || is_hc_in_state(hc_state_enum.HC_OVERLOADED)) {
			notify("Configuration file was processed on all HCs.");
			mc_state = reconf ? mcStateEnum.MC_READY : mcStateEnum.MC_ACTIVE;
		} else {
			error("There is no HC connection after processing the configuration file.");
			mc_state = mcStateEnum.MC_LISTENING;
		}
	}

	private void add_component_to_host(final Host host, final ComponentStruct component) {
		if (component.comp_ref == TitanComponent.MTC_COMPREF) {
			component.log_source = MessageFormat.format("MTC@{0}", host.hostname_local);
		} else if (component.comp_name != null) {
			component.log_source = MessageFormat.format("{0}({1})@{2}",
					component.comp_name, component.comp_ref, host.hostname_local);
		} else {
			component.log_source = MessageFormat.format("{0}@{1}", component.comp_ref, host.hostname_local);
		}

		component.comp_location = host;
		int i;
		for (i = host.components.size(); i > 0; i--) {
			if (host.components.get(i - 1) < component.comp_ref) {
				break;
			} else if (host.components.get(i - 1) == component.comp_ref) {
				return;
			}
		}
		host.components.add(i, component.comp_ref);
	}

	private static void remove_component_from_host(final ComponentStruct component) {
		component.log_source = null;
		final Host host = component.comp_location;
		if (host != null) {
			int i;
			for (i = host.components.size() - 1; i >= 0; i--) {
				if (host.components.get(i) == component.comp_ref) {
					break;
				} else if (host.components.get(i) < component.comp_ref) {
					return;
				}
			}

			if (i < 0) {
				return;
			}

			host.components.remove(i);
		}
	}

	private void close_hc_connection(final Host hc) {
		if (hc.hc_state != hc_state_enum.HC_DOWN) {
			try {
				hc.socket.close();
			} catch (IOException e) {
				// FIXME
			}
			channel_table.remove(hc.socket);
			hc.socket = null;
			hc.text_buf = null;
			hc.hc_state = hc_state_enum.HC_DOWN;
		}
	}

	private boolean is_hc_in_state(final hc_state_enum checked_state) {
		for (final Host host: hosts) {
			if (host.hc_state == checked_state) {
				return true;
			}
		}

		return false;
	}

	private boolean all_hc_in_state(final hc_state_enum checked_state) {
		for (final Host host: hosts) {
			if (host.hc_state != checked_state) {
				return false;
			}
		}

		return true;
	}

	private static int receive_to_buffer(final SocketChannel channel, final Text_Buf text_buf, final boolean receive_from_socket) {
		if (!receive_from_socket) {
			return 1;
		}

		final AtomicInteger buf_ptr = new AtomicInteger();
		final AtomicInteger buf_len = new AtomicInteger();
		text_buf.get_end(buf_ptr, buf_len);

		final ByteBuffer tempbuffer = ByteBuffer.allocate(1024);
		int recv_len = 0;
		try {
			recv_len = channel.read(tempbuffer);
		} catch (IOException e) {
			e.printStackTrace();
			throw new TtcnError(e);
		}
		if (recv_len > 0) {
			// text_buf.increase_length(recv_len);
			text_buf.push_raw(recv_len, tempbuffer.array());
		}

		return recv_len;
	}

	private boolean check_version(final unknown_connection connection) {
		final Text_Buf text_buf = connection.text_buf;
		final int version_major = text_buf.pull_int().get_int();
		final int version_minor = text_buf.pull_int().get_int();
		final int version_patchlevel = text_buf.pull_int().get_int();
		if (version_major != TTCN_Runtime.TTCN3_MAJOR || version_minor != TTCN_Runtime.TTCN3_MINOR
				|| version_patchlevel != TTCN_Runtime.TTCN3_PATCHLEVEL) {
			send_error(connection.channel, MessageFormat.format("Version mismatch: The TTCN-3 Main Controller has version {0}, but the ETS was built with version {1}.{2}.pl{3}.",
					TTCN_Runtime.PRODUCT_NUMBER, version_major, version_minor, version_patchlevel));
			return true;
		}

		final int version_build_number = text_buf.pull_int().get_int();
		if (version_build_number != TTCN_Runtime.TTCN3_BUILDNUMBER) {
			if (version_build_number > 0) {
				send_error(connection.channel, MessageFormat.format("Build number mismatch: The TTCN-3 Main Controller has version {0}, but the ETS was built with {1}.{2}.pre{3} build {4}.",
						TTCN_Runtime.PRODUCT_NUMBER, version_major, version_minor, version_patchlevel, version_build_number));
			} else {
				send_error(connection.channel, MessageFormat.format("Build number mismatch: The TTCN-3 Main Controller has version {0}, but the ETS was built with {1}.{2}.pl{3}.",
						TTCN_Runtime.PRODUCT_NUMBER, version_major, version_minor, version_patchlevel));
			}

			return true;
		}

		if (!modules.isEmpty()) {
			//the version is known
			final int new_modules_size = text_buf.pull_int().get_int();
			if (modules.size() != new_modules_size) {
				send_error(connection.channel, MessageFormat.format("The number of modules in this ETS ({0}) differs from the number of modules in the firstly connected ETS ({1}).", new_modules_size, modules.size()));
				return true;
			}
			for (int i = 0; i < new_modules_size; i++) {
				final String module_name = text_buf.pull_string();
				module_version_info other_module = null;
				for (int j = 0; j < modules.size(); j++) {
					if (module_name.equals(modules.get(j).module_name)) {
						other_module = modules.get(j);
						break;
					}
				}
				if (other_module == null) {
					send_error(connection.channel, MessageFormat.format("The module number {0} in this ETS ({1}) has different name than any other module in the firstly connected ETS.", i, module_name));
					return true;
				}

				boolean checksum_differs = false;
				final int checksum_length = text_buf.pull_int().get_int();
				byte[] module_checksum;
				if (checksum_length > 0) {
					module_checksum = new byte[checksum_length];
					text_buf.pull_raw(checksum_length, module_checksum);
				} else {
					module_checksum = null;
				}
				if (checksum_length != other_module.module_checksum.length) {
					send_error(connection.channel, MessageFormat.format("The checksum of module {0} in this ETS has different length ({1}) than that of the firstly connected ETS ({2}).",
							module_name, checksum_length, other_module.module_checksum.length));
					return true;
				}

				if (!Arrays.equals(module_checksum, other_module.module_checksum)) {
					for (int j = 0; j < checksum_length; j++) {
						if (module_checksum[j] != other_module.module_checksum[j]) {
							send_error(connection.channel, MessageFormat.format("At index {0} the checksum of module {1} in this ETS is different ({2}) than that of the firstly connected ETS ({3}).",
									j, module_name, module_checksum[j], other_module.module_checksum[j]));
							checksum_differs = true;
						}
					}

					if (checksum_differs) {
						send_error(connection.channel, MessageFormat.format("The checksum of module {0} in this ETS is different than that of the firstly connected ETS.",
								module_name));
					}
				}

				if (checksum_differs) {
					return true;
				}
			}
		} else {
			final int modules_size = text_buf.pull_int().get_int();
			for (int i = 0; i < modules_size; i++) {
				final module_version_info temp_info = new module_version_info();
				temp_info.module_name = text_buf.pull_string();
				final int checksum_length = text_buf.pull_int().get_int();
				if (checksum_length > 0) {
					temp_info.module_checksum = new byte[checksum_length];
					text_buf.pull_raw(checksum_length, temp_info.module_checksum);
				} else {
					temp_info.module_checksum = new byte[0];
				}

				modules.add(temp_info);
			}
		}

		return false;
	}

	private void process_version(final unknown_connection connection) {
		if (check_version(connection)) {
			error(MessageFormat.format("HC connection from {0} [{1}] was refused because of incorrect version.",
					connection.ip_address.getCanonicalHostName(), connection.ip_address.getHostAddress()));
			close_unknown_connection(connection);
			return;
		}

		final Host hc = add_new_host(connection);
		switch (mc_state) {
		case MC_LISTENING:
			mc_state = mcStateEnum.MC_HC_CONNECTED;
		case MC_HC_CONNECTED:
			break;
		case MC_LISTENING_CONFIGURED:
		case MC_ACTIVE:
			configure_host(hc, true);
			mc_state = mcStateEnum.MC_CONFIGURING;
			break;
		case MC_SHUTDOWN:
			send_exit_hc(hc);
			hc.hc_state = hc_state_enum.HC_EXITING;
			break;
		default:
			configure_host(hc, true);
		}

		handle_hc_data(hc, false);
		status_change();
	}

	private Host add_new_host(final unknown_connection connection) {
		final Text_Buf text_buf = connection.text_buf;
		final SocketChannel channel = connection.channel;

		final Host hc = new Host();
		hc.ip_address = channel.socket().getInetAddress();
		hc.hostname = hc.ip_address.getCanonicalHostName();
		hc.hostname_local = text_buf.pull_string();
		hc.machine_type = text_buf.pull_string();
		hc.system_name = text_buf.pull_string();
		hc.system_release = text_buf.pull_string();
		hc.system_version = text_buf.pull_string();

		for (int i = 0; i < transport_type_enum.TRANSPORT_NUM.ordinal(); i++) {
			hc.transport_supported[i] = false;
		}

		final int n_supported_transports = text_buf.pull_int().get_int();
		for (int i = 0; i < n_supported_transports; i++) {
			final int transport_type = text_buf.pull_int().get_int();
			if (transport_type >= 0 && transport_type < transport_type_enum.TRANSPORT_NUM.ordinal()) {
				if (hc.transport_supported[transport_type]) {
					send_error(channel, MessageFormat.format("Malformed VERSION message was received: Transport type {0} "
							+ " was specified more than once.", transport_type_enum.values()[transport_type].toString()));
				} else {
					hc.transport_supported[transport_type] = true;
				}
			} else {
				send_error(channel, MessageFormat.format("Malformed VERSION message was received: Transport type code {0} "
						+ "is invalid.", transport_type));
			}
		}

		if (!hc.transport_supported[transport_type_enum.TRANSPORT_LOCAL.ordinal()]) {
			send_error(channel, MessageFormat.format("Malformed VERSION message was received: Transport type {0} "
					+ " must be supported anyway.", transport_type_enum.TRANSPORT_LOCAL.toString()));
		}
		if (!hc.transport_supported[transport_type_enum.TRANSPORT_INET_STREAM.ordinal()]) {
			send_error(channel, MessageFormat.format("Malformed VERSION message was received: Transport type {0} "
					+ " must be supported anyway.", transport_type_enum.TRANSPORT_INET_STREAM.toString()));
		}
		if (hc.transport_supported[transport_type_enum.TRANSPORT_UNIX_STREAM.ordinal()]) {
			hc.transport_supported[transport_type_enum.TRANSPORT_UNIX_STREAM.ordinal()] = false;
		}

		hc.log_source = MessageFormat.format("HC@{0}", hc.hostname_local);
		hc.hc_state = hc_state_enum.HC_IDLE;
		hc.socket = channel;
		hc.text_buf = text_buf;
		// FIXME add is_similar_hostname
		add_allowed_components(hc);
		hc.n_active_components = 0;

		text_buf.cut_message();

		delete_unknown_connection(connection);

		hosts.add(hc);
		final channel_table_struct new_struct = new channel_table_struct();
		new_struct.channel_type = channel_type_enum.CHANNEL_HC;
		new_struct.host = hc;
		channel_table.put(channel, new_struct);

		notify(MessageFormat.format("New HC connected from {0} [{1}]. {2}: {3} {4} on {5}.",
				hc.hostname, hc.ip_address.getHostAddress(),
				hc.hostname_local, hc.system_name,
				hc.system_release, hc.machine_type));

		return hc;
	}

	public void configure(final String config_file_content) {
		lock();
		switch (mc_state) {
		case MC_HC_CONNECTED:
		case MC_ACTIVE:
			mc_state = mcStateEnum.MC_CONFIGURING;
			break;
		case MC_LISTENING:
		case MC_LISTENING_CONFIGURED:
			mc_state = mcStateEnum.MC_LISTENING_CONFIGURED;
			break;
		case MC_RECONFIGURING:
			break;
		default:
			error("MainController::configure: called in wrong state.");
			unlock();
			return;
		}

		// FIXME needs to be processed somewhere
		config_str = config_file_content;

		if (mc_state == mcStateEnum.MC_CONFIGURING || mc_state == mcStateEnum.MC_RECONFIGURING) {
			notify("Downloading configuration file to all HCs.");
			for (final Host host : hosts) {
				configure_host(host, false);
			}
		}

		if (mc_state == mcStateEnum.MC_RECONFIGURING) {
			notify("Downloading configuration file to the MTC.");
			configure_mtc();
		}

		status_change();
		unlock();
	}

	private void configure_mtc() {
		if (config_str == null) {
			fatal_error("MainController.configure_mtc: no config file");
		}
		if (mtc.tc_state == tc_state_enum.TC_IDLE) {
			error("MainController.configure_mtc(): MTC is in wrong state.");
		} else {
			mtc.tc_state = tc_state_enum.MTC_CONFIGURING;
			send_configure_mtc(config_str);
		}

	}

	private void send_configure_mtc(final String config) {
		final Text_Buf text_buf = new Text_Buf();
		text_buf.push_int(MSG_CONFIGURE);
		text_buf.push_string(config);
		send_message(mtc.socket, text_buf);
	}

	private void configure_host(final Host host, final boolean should_notify) {
		hc_state_enum next_state = hc_state_enum.HC_CONFIGURING;
		switch (host.hc_state) {
		case HC_CONFIGURING:
		case HC_CONFIGURING_OVERLOADED:
		case HC_EXITING:
			fatal_error(MessageFormat.format("MainController.configure_host(): host {0} is in wrong state.",
					host.hostname));
			break;
		case HC_DOWN:
			break;
		case HC_OVERLOADED:
			next_state = hc_state_enum.HC_CONFIGURING_OVERLOADED;
			// no break
		default:
			host.hc_state = next_state;
			if (should_notify) {
				notify(MessageFormat.format("Downloading configuration file to HC on host {0}.", host.hostname));
			}
			send_configure(host);

			if (mc_state != mcStateEnum.MC_RECONFIGURING) {
				// TODO send debug setup
			}
		}
	}

	private void send_configure(final Host hc) {
		final Text_Buf text_buf = new Text_Buf();
		text_buf.push_int(MSG_CONFIGURE);
		text_buf.push_string(config_str);

		send_message(hc.socket, text_buf);
	}

	private void send_message(final SocketChannel channel, final Text_Buf text_buf) {
		text_buf.calculate_length();
		final byte[] msg_ptr = text_buf.get_data();
		final int msg_len = text_buf.get_len();
		final ByteBuffer buffer = ByteBuffer.wrap(msg_ptr, text_buf.get_begin(), msg_len);
		try {
			while (buffer.hasRemaining()) {
				channel.write(buffer);
			}
		} catch (IOException e) {
			final StringWriter error = new StringWriter();
			e.printStackTrace(new PrintWriter(error));
			error(MessageFormat.format("Sending of message failed:  {0}", error.toString()));
		}
	}

	private void process_mtc_created(final unknown_connection connection) {
		if (mc_state != mcStateEnum.MC_CREATING_MTC) {
			send_error(connection.channel, "Message MTC_CREATED arrived in invalid state.");
			close_unknown_connection(connection);
			return;
		}

		if (mtc == null || mtc.tc_state != tc_state_enum.TC_INITIAL) {
			fatal_error("MainController::process_mtc_created: MTC is in invalid state.");
			close_unknown_connection(connection);
			return;
		}
		// FIXME: implement ip_address check

		mc_state = mcStateEnum.MC_READY;
		mtc.tc_state = tc_state_enum.TC_IDLE;
		mtc.socket = connection.channel;

		final channel_table_struct new_struct = new channel_table_struct();
		new_struct.channel_type = channel_type_enum.CHANNEL_TC;
		new_struct.component = mtc;
		final Text_Buf text_buf = connection.text_buf;
		text_buf.cut_message();
		mtc.text_buf = text_buf;
		channel_table.put(connection.channel, new_struct);

		delete_unknown_connection(connection);

		notify("MTC is created.");

		// handle the remaining messages that are in text_buf
		handle_tc_data(mtc, false);
		status_change();
	}

	private void handle_tc_data(final ComponentStruct tc, final boolean receive_from_socket) {
		final Text_Buf text_buf = tc.text_buf;
		boolean close_connection = false;
		final int recv_len = receive_to_buffer(tc.socket, text_buf, receive_from_socket);

		if (recv_len >= 0) {
			try {
				while (text_buf.is_message()) {
					final int msg_len = text_buf.pull_int().get_int();
					final int msg_end = text_buf.get_pos() + msg_len;
					final int msg_type = text_buf.pull_int().get_int();
					switch (msg_type) {
					case MSG_ERROR:
						process_error(tc);
						break;
					case MSG_LOG:
						process_log(tc);
						break;
					case MSG_CREATE_REQ:
						process_create_req(tc);
						break;
					case MSG_START_REQ:
						process_start_req(tc);
						break;
					case MSG_STOP_REQ:
						process_stop_req(tc);
						break;
					case MSG_KILL_REQ:
						process_kill_req(tc);
						break;
					case MSG_IS_RUNNING:
						process_is_running(tc);
						break;
					case MSG_IS_ALIVE:
						process_is_alive(tc);
						break;
					case MSG_DONE_REQ:
						process_done_req(tc);
						break;
					case MSG_KILLED_REQ:
						process_killed_req(tc);
						break;
					case MSG_CANCEL_DONE_ACK:
						process_cancel_done_ack(tc);
						break;
					case MSG_CONNECT_REQ:
						process_connect_req(tc);
						break;
					case MSG_CONNECT_LISTEN_ACK:
						process_connect_listen_ack(tc, msg_end); //FIXME: check addr part
						break;
					case MSG_CONNECTED:
						process_connected(tc);
						break;
					case MSG_CONNECT_ERROR:
						process_connect_error(tc);
						break;
					case MSG_DISCONNECT_REQ:
						process_disconnect_req(tc);
						break;
					case MSG_DISCONNECTED:
						process_disconnected(tc);
						break;
					case MSG_MAP_REQ:
						process_map_req(tc);
						break;
					case MSG_MAPPED:
						process_mapped(tc);
						break;
					case MSG_UNMAP_REQ:
						process_unmap_req(tc);
						break;
					case MSG_UNMAPPED:
						process_unmapped(tc);
						break;
					case MSG_DEBUG_RETURN_VALUE:
						// TODO: process_debug_return_value(*tc->text_buf, tc->log_source, message_end,tc == mtc);
						break;
					case MSG_DEBUG_HALT_REQ:
						// TODO: process_debug_broadcast_req(tc, D_HALT);
						break;
					case MSG_DEBUG_CONTINUE_REQ:
						// TODO: process_debug_broadcast_req(tc, D_CONTINUE);
						break;
					case MSG_DEBUG_BATCH:
						// TODO: process_debug_batch(tc);
						break;
					default:
						if (tc == mtc) {
							// these messages can be received only from the MTC
							switch (msg_type) {
							case MSG_TESTCASE_STARTED:
								process_testcase_started();
								break;
							case MSG_TESTCASE_FINISHED:
								process_testcase_finished();
								break;
							case MSG_MTC_READY:
								process_mtc_ready();
								break;
							case MSG_CONFIGURE_ACK:
								process_configure_ack_mtc();
								break;
							case MSG_CONFIGURE_NAK:
								process_configure_nak_mtc();
								break;
							default:
								error(MessageFormat.format("Invalid message type ({0}) was received from the MTC at {1} [{2}].",
										msg_type, mtc.comp_location.hostname, mtc.comp_location.ip_address.getHostAddress()));
								close_connection = true;
							}
						} else {
							// these messages can be received only from PTCs
							switch (msg_type) {
							case MSG_STOPPED:
								process_stopped(tc, msg_end);
								break;
							case MSG_STOPPED_KILLED:
								process_stopped_killed(tc, msg_end);
								break;
							case MSG_KILLED:
								process_killed(tc);
								break;
							default:
								notify(MessageFormat.format("Invalid message type ({0}) was received from PTC {1} "
										+ "at {2} [{3}].", msg_type, tc.comp_ref, tc.comp_location.hostname,
										tc.comp_location.ip_address.getHostAddress()));
								close_connection = true;
							}
						}
					}

					if (close_connection) {
						break;
					}

					text_buf.cut_message();
				}
			} catch (TtcnError e) {
				if (tc == mtc) {
					error(MessageFormat.format("Malformed message was received from the MTC at {0} [{1}].",
							mtc.comp_location.hostname, mtc.comp_location.ip_address.getHostAddress()));
				} else {
					notify(MessageFormat.format("Malformed message was received from PTC {0} at {1} [{2}].",
							tc.comp_ref, tc.comp_location.hostname, tc.comp_location.ip_address.getHostAddress()));
				}
				close_connection = true;
			}

			if (close_connection) {
				send_error(tc.socket, "The received message was not understood by the MC.");
			}
		} else if (recv_len == -1) {
			//This is different from the C side
			//On the C side a readable file descriptor with 0 bytes to read means that the connection is being closed
			//In Java it is possible to have a readable socket, before any bytes have arrived to it.
			//tCP connection is closed by peer
			if (tc.tc_state != tc_state_enum.TC_EXITING && !tc.process_killed) {
				if (tc == mtc) {
					error(MessageFormat.format("Unexpected end of MTC connection from {0} [{1}].",
							mtc.comp_location.hostname, mtc.comp_location.ip_address.getHostAddress()));
				} else {
					notify(MessageFormat.format("Unexpected end of PTC connection {0} from {1} [{2}].",
							tc.comp_ref, tc.comp_location.hostname, tc.comp_location.ip_address.getHostAddress()));
				}
			}

			close_connection = true;
		} else {
			// FIXME handle ECONNRESET
			if (tc == mtc) {
				//TODO what to print instead of errno
				error(MessageFormat.format("Receiving of data failed from the MTC at {0} [{1}]",
						mtc.comp_location.hostname, mtc.comp_location.ip_address.getHostAddress()));
			} else {
				//TODO what to print instead of errno
				notify(MessageFormat.format("Receiving of data failed from PTC {0} at {1} [{2}]",
						tc.comp_ref, tc.comp_location.hostname, tc.comp_location.ip_address.getHostAddress()));
			}
			close_connection = true;
		}

		if (close_connection) {
			close_tc_connection(tc);
			remove_component_from_host(tc);
			if (tc == mtc) {
				if (mc_state != mcStateEnum.MC_TERMINATING_MTC) {
					notify("The control connection to MTC is lost. Destroying all PTC connections.");
				}

				destroy_all_components();
				notify("MTC terminated");
				if (is_hc_in_state(hc_state_enum.HC_CONFIGURING)) {
					mc_state = mcStateEnum.MC_CONFIGURING;
				} else if (is_hc_in_state(hc_state_enum.HC_IDLE)) {
					mc_state = mcStateEnum.MC_HC_CONNECTED;
				} else if (is_hc_in_state(hc_state_enum.HC_ACTIVE)
						|| is_hc_in_state(hc_state_enum.HC_OVERLOADED)) {
					mc_state = mcStateEnum.MC_ACTIVE;
				} else {
					mc_state = mcStateEnum.MC_LISTENING_CONFIGURED;
				}
				stop_requested = false;
			} else {
				if (tc.tc_state != tc_state_enum.TC_EXITING) {
					// we have no idea about the final verdict of the PTC
					tc.local_verdict = VerdictTypeEnum.ERROR;
					component_terminated(tc);
				}

				tc.tc_state = tc_state_enum.TC_EXITED;
				if (mc_state == mcStateEnum.MC_TERMINATING_TESTCASE
						&& ready_to_finish_testcase()) {
					finish_testcase();
				}
			}

			status_change();
		}
	}

	private void close_tc_connection(final ComponentStruct component) {
		if (component.socket != null) {
			try {
				component.socket.close();
			} catch (IOException e) {
				// FIXME handle
			}

			channel_table.remove(component.socket);
			component.socket = null;
			component.text_buf = null;
		}

		if (component.kill_timer != null) {
			cancel_timer(component.kill_timer);
			component.kill_timer = null;
		}
	}

	private boolean ready_to_finish_testcase() {
		for (int i = tc_first_comp_ref; i < components.size(); i++) {
			final ComponentStruct comp = components.get(i);
			switch (comp.tc_state) {
			case TC_EXITED:
			case PTC_STALE:
				break;
			default:
				return false;
			}
		}

		return true;
	}

	private void process_stopped(final ComponentStruct tc, final int msg_end) {
		switch (tc.tc_state) {
		case TC_STOPPING:
		case PTC_FUNCTION:
		case PTC_STOPPING_KILLING:
			// only alive PTCs are allowed to send STOPPED
			if (tc.is_alive) {
				break;
			}
		default:
			send_error(tc.socket, "Unexpected message STOPPED was received.");
			return;
		}

		final Text_Buf text_buf = tc.text_buf;
		tc.local_verdict = VerdictTypeEnum.values()[text_buf.pull_int().get_int()];
		tc.verdict_reason = text_buf.pull_string();
		tc.return_type = text_buf.pull_string();
		// FIXME: tc->return_value_len = message_end - text_buf.get_pos();
		tc.return_value = new byte[msg_end - text_buf.get_pos()];
		text_buf.pull_raw(tc.return_value.length, tc.return_value);

		// FIXME: free_qualified_name(&tc->tc_fn_name);
		tc.tc_fn_name = new QualifiedName("", "");

		component_stopped(tc);
		status_change();
	}

	private void component_stopped(final ComponentStruct tc) {
		// checking and updating the state of tc
		final tc_state_enum old_state = tc.tc_state;
		if (old_state == tc_state_enum.PTC_STOPPING_KILLING) {
			tc.tc_state = tc_state_enum.PTC_KILLING;
		} else {
			tc.tc_state = tc_state_enum.PTC_STOPPED;
			if (tc.kill_timer != null) {
				cancel_timer(tc.kill_timer);
				tc.kill_timer = null;
			}
		}

		switch (mc_state) {
		case MC_EXECUTING_TESTCASE:
			// this is the correct state
			break;
		case MC_TERMINATING_TESTCASE:
			// do nothing, we are waiting for the end of all PTC connections
			return;
		default:
			error(MessageFormat.format("PTC {0} stopped in invalid MC state.", tc.comp_ref));
			return;
		}
		if (!tc.is_alive) {
			send_error(tc.socket, "Message STOPPED can only be sent by alive PTCs.");
			return;
		}

		// Note: the COMPONENT_STATUS message must be sent before STOP_ACK because
		// the latter may update the component status cache table to an inconsistent state
		boolean send_status_to_mtc = false;
		boolean send_done_to_mtc = false;
		// sending out COMPONENT_STATUS messages to PTCs
		for (int i = 0;; i++) {
			final ComponentStruct requestor = get_requestor(tc.done_requestors, i);
			if (requestor == null) {
				break;
			} else if (requestor.equals(mtc)) {
				send_status_to_mtc = true;
				send_done_to_mtc = true;
			} else {
				send_component_status_to_requestor(tc, requestor, true, false);
			}
		}

		// do not send unsolicited 'any component.done' status
		if (any_component_done_requested) {
			send_status_to_mtc = true;
		}
		boolean all_done_checked = false;
		boolean all_done_result = false;
		if (all_component_done_requested) {
			all_done_checked = true;
			all_done_result = !is_any_component_running();
			if (all_done_result) {
				send_status_to_mtc = true;
			}
		}

		if (send_status_to_mtc) {
			if (!all_done_checked) {
				all_done_result = !is_any_component_running();
			}
			if (send_done_to_mtc) {
				// the return value was requested
				send_component_status_mtc(tc.comp_ref, true, false, any_component_done_requested, all_done_result,
						false, false, tc.local_verdict, tc.return_type, tc.return_value);
			} else {
				send_component_status_mtc(TitanComponent.NULL_COMPREF, false, false, any_component_done_requested,
						all_done_result, false, false, VerdictTypeEnum.NONE, null, null);
			}
			if (any_component_done_requested) {
				any_component_done_requested = false;
				any_component_done_sent = true;
			}
			if (all_done_result) {
				all_component_done_requested = false;
			}
		}

		// sending out STOP_ACK messages
		if (old_state != tc_state_enum.PTC_FUNCTION) {
			// the PTC was explicitly stopped and/or killed
			if (mtc.tc_state == tc_state_enum.MTC_ALL_COMPONENT_KILL) {
				// do nothing
			} else if (mtc.tc_state == tc_state_enum.MTC_ALL_COMPONENT_STOP) {
				check_all_component_stop();
			} else {
				send_stop_ack_to_requestors(tc);
			}
		}
	}

	private void process_unmapped(final ComponentStruct tc) {
		if (!message_expected(tc, "UNMAPPED")) {
			return;
		}

		final Text_Buf text_buf = tc.text_buf;
		final boolean translation = text_buf.pull_int().get_int() == 0 ? false : true;
		final String sourcePort = text_buf.pull_string();
		final String systemPort = text_buf.pull_string();
		final int nof_params = text_buf.pull_int().get_int();
		final Map_Params params = new Map_Params(nof_params);
		for (int i = 0; i < nof_params; i++) {
			final String par = text_buf.pull_string();
			params.set_param(i, new TitanCharString(par));
		}

		PortConnection conn = null;
		if (!translation) {
			conn = find_connection(tc.comp_ref, sourcePort, TitanComponent.SYSTEM_COMPREF, systemPort);
		} else {
			conn = find_connection(TitanComponent.SYSTEM_COMPREF, sourcePort, tc.comp_ref, systemPort);
		}

		if (conn != null) {
			switch (conn.conn_state) {
			case CONN_MAPPING:
			case CONN_MAPPED:
			case CONN_UNMAPPING:
				destroy_mapping(conn, nof_params, params, null);
				break;
			default:
				send_error(tc.socket, MessageFormat.format("Unexpected MAPPED message was received for "
						+ "port mapping {0}:{1} - system:{2}.", tc.comp_ref, sourcePort, systemPort));
			}
		}
		status_change();
	}

	private void process_unmap_req(final ComponentStruct tc) {
		if (!request_allowed(tc, "UNMAP_REQ")) {
			return;
		}

		final Text_Buf text_buf = tc.text_buf;
		final int sourceComponent = text_buf.pull_int().get_int();
		final boolean translation = text_buf.pull_int().get_int() == 0 ? false : true;
		final String sourcePort = text_buf.pull_string();
		final String systemPort = text_buf.pull_string();

		if (!valid_endpoint(sourceComponent, false, tc, "unmap")) {
			return;
		}

		final int nof_params = text_buf.pull_int().get_int();

		final Map_Params params = new Map_Params(nof_params);
		for (int i = 0; i < nof_params; i++) {
			final String par = text_buf.pull_string();
			params.set_param(i, new TitanCharString(par));
		}

		final PortConnection conn = find_connection(sourceComponent, sourcePort, TitanComponent.SYSTEM_COMPREF, systemPort);
		if (conn == null) {
			send_unmap_ack(tc, nof_params, params);
		} else {
			switch (conn.conn_state) {
			case CONN_MAPPED:
				send_unmap(components.get(sourceComponent), sourcePort, systemPort, nof_params, params, translation);
				conn.conn_state = conn_state_enum.CONN_UNMAPPING;
				// no break
			case CONN_UNMAPPING:
				add_requestor(conn.requestors, tc);
				tc.tc_state = tc_state_enum.TC_UNMAP;
				status_change();
				break;
			case CONN_MAPPING:
				send_error(tc.socket, MessageFormat.format("The port mapping {0}:{1} - system:{2} cannot be "
						+ "destroyed because a map operation is in progress on it.", sourceComponent, sourcePort, systemPort));
				break;
			default:
				send_error(tc.socket, MessageFormat.format("The port mapping {0}:{1} - system:{2} is in invalid "
						+ "state.", sourceComponent, sourcePort, systemPort));
			}
		}
	}

	private void send_unmap(final ComponentStruct tc, final String sourcePort, final String systemPort, final int nof_params,
			final Map_Params params, final boolean translation) {

		final Text_Buf text_buf = new Text_Buf();
		text_buf.push_int(MSG_UNMAP);
		text_buf.push_int(translation ? 1 : 0);
		text_buf.push_string(sourcePort);
		text_buf.push_string(systemPort);
		text_buf.push_int(nof_params);
		for (int i = 0; i < nof_params; i++) {
			text_buf.push_string(params.get_param(i).get_value().toString());
		}

		send_message(tc.socket, text_buf);
	}

	private void process_killed_req(final ComponentStruct tc) {
		if (!request_allowed(tc, "KILLED_REQ")) {
			return;
		}

		final Text_Buf text_buf = tc.text_buf;
		final int component_reference = text_buf.pull_int().get_int();

		switch (component_reference) {
		case TitanComponent.NULL_COMPREF:
			send_error(tc.socket, "Killed operation was requested on the null component reference.");
			return;
		case TitanComponent.MTC_COMPREF:
			send_error(tc.socket, "Killed operation was requested on the component reference of the MTC.");
			return;
		case TitanComponent.SYSTEM_COMPREF:
			send_error(tc.socket, "Killed operation was requested on the component reference of the system.");
			return;
		case TitanComponent.ANY_COMPREF:
			if (tc.equals(mtc)) {
				final boolean answer = !is_all_component_alive();
				send_killed_ack(mtc, answer);
				if (!answer) {
					any_component_killed_requested = true;
				}
			} else {
				send_error(tc.socket, "Operation 'any component.killed' can only be performed on the MTC.");
			}
			return;
		case TitanComponent.ALL_COMPREF:
			if (tc == mtc) {
				final boolean answer = !is_any_component_alive();
				send_killed_ack(mtc, answer);
				if (!answer) {
					all_component_killed_requested = true;
				}
			} else {
				send_error(tc.socket, "Operation 'all component.killed' can only be performed on the MTC.");
			}
			return;
		default:
			break;
		}

		// the operation refers to a specific PTC
		final ComponentStruct comp = lookup_component(component_reference);
		if (comp == null) {
			send_error(tc.socket, MessageFormat.format("The argument of killed operation is an invalid "
					+ "component reference: {0}.", component_reference));
			return;
		}
		switch (comp.tc_state) {
		case TC_EXITING:
		case TC_EXITED:
			send_killed_ack(tc, true);
			break;
		case TC_IDLE:
		case TC_CREATE:
		case TC_START:
		case TC_STOP:
		case TC_KILL:
		case TC_CONNECT:
		case TC_DISCONNECT:
		case TC_MAP:
		case TC_UNMAP:
		case TC_STOPPING:
		case PTC_FUNCTION:
		case PTC_STARTING:
		case PTC_STOPPED:
		case PTC_KILLING:
		case PTC_STOPPING_KILLING:
			send_killed_ack(tc, false);
			add_requestor(comp.killed_requestors, tc);
			break;
		case PTC_STALE:
			send_error(tc.socket, MessageFormat.format("The argument of killed operation ({0}) is a component "
					+ "reference that belongs to an earlier testcase.", component_reference));
			break;
		default:
			send_error(tc.socket, MessageFormat.format("The test component that the killed operation refers to "
					+ "({0}) is in invalid state.", component_reference));
		}
	}

	private void send_killed_ack(final ComponentStruct tc, final boolean answer) {
		final Text_Buf text_buf = new Text_Buf();
		text_buf.push_int(MSG_KILLED_ACK);
		text_buf.push_int(answer ? 1 : 0);

		send_message(tc.socket, text_buf);
	}

	private void process_done_req(final ComponentStruct tc) {
		if (!request_allowed(tc, "DONE_REQ")) {
			return;
		}

		final Text_Buf text_buf = tc.text_buf;
		final int component_reference = text_buf.pull_int().get_int();

		switch (component_reference) {
		case TitanComponent.NULL_COMPREF:
			send_error(tc.socket, "Done operation was requested on the null component reference.");
			return;
		case TitanComponent.MTC_COMPREF:
			send_error(tc.socket, "Done operation was requested on the component reference of the MTC.");
			return;
		case TitanComponent.SYSTEM_COMPREF:
			send_error(tc.socket, "Done operation was requested on the component reference of the system.");
			return;
		case TitanComponent.ANY_COMPREF:
			if (tc.equals(mtc)) {
				final boolean answer = is_any_component_done();
				send_done_ack(mtc, answer, VerdictTypeEnum.NONE, null, null);
				if (answer) {
					any_component_done_sent = true;
				} else {
					any_component_done_requested = true;
				}
			} else {
				send_error(tc.socket, "Operation 'any component.done' can only be performed on the MTC.");
			}
			return;
		case TitanComponent.ALL_COMPREF:
			if (tc.equals(mtc)) {
				final boolean answer = !is_any_component_running();
				send_done_ack(mtc, answer, VerdictTypeEnum.NONE, null, null);
				if (!answer) {
					all_component_done_requested = true;
				}
			} else {
				send_error(tc.socket, "Operation 'all component.done' can only be performed on the MTC.");
			}
			return;
		default:
			break;
		}

		// the operation refers to a specific PTC
		final ComponentStruct comp = lookup_component(component_reference);
		if (comp == null) {
			send_error(tc.socket, MessageFormat.format("The argument of done operation is an invalid component "
					+ "reference: {0}.", component_reference));
			return;
		}

		switch (comp.tc_state) {
		case PTC_STOPPED:
			// this answer has to be cancelled when the component is re-started
			add_requestor(comp.done_requestors, tc);
			// no break
		case TC_EXITING:
		case TC_EXITED:
		case PTC_KILLING:
			send_done_ack(tc, true, comp.local_verdict, comp.return_type, comp.return_value);
			break;
		case TC_IDLE:
		case TC_CREATE:
		case TC_START:
		case TC_STOP:
		case TC_KILL:
		case TC_CONNECT:
		case TC_DISCONNECT:
		case TC_MAP:
		case TC_UNMAP:
		case TC_STOPPING:
		case PTC_FUNCTION:
		case PTC_STARTING:
		case PTC_STOPPING_KILLING:
			send_done_ack(tc, false, VerdictTypeEnum.NONE, null, null);
			add_requestor(comp.done_requestors, tc);
			break;
		case PTC_STALE:
			send_error(tc.socket, MessageFormat.format("The argument of done operation ({0}) "
					+ "is a component reference that belongs to an earlier testcase.", component_reference));
			break;
		default:
			send_error(tc.socket, MessageFormat.format("The test component that the done operation refers to ({0}) "
					+ "is in invalid state.", component_reference));
		}
	}

	private void send_done_ack(final ComponentStruct tc, final boolean answer, final VerdictTypeEnum verdict, final String return_type, final byte[] return_value) {
		final Text_Buf text_buf = new Text_Buf();
		text_buf.push_int(MSG_DONE_ACK);
		text_buf.push_int(answer ? 1 : 0);
		text_buf.push_int(verdict.getValue());
		text_buf.push_string(return_type);
		if (return_value != null) {
			text_buf.push_raw(return_value.length, return_value);
		}
		send_message(tc.socket, text_buf);
	}

	private void process_cancel_done_ack(final ComponentStruct tc) {
		final Text_Buf text_buf = tc.text_buf;
		final int component_reference = text_buf.pull_int().get_int();

		switch (component_reference) {
		case TitanComponent.NULL_COMPREF:
			send_error(tc.socket, "Message CANCEL_DONE_ACK refers to the null component reference.");
			return;
		case TitanComponent.MTC_COMPREF:
			send_error(tc.socket, "Message CANCEL_DONE_ACK refers to the component reference of the MTC.");
			return;
		case TitanComponent.SYSTEM_COMPREF:
			send_error(tc.socket, "Message CANCEL_DONE_ACK refers to the component reference of the system.");
			return;
		case TitanComponent.ANY_COMPREF:
			send_error(tc.socket, "Message CANCEL_DONE_ACK refers to 'any component'.");
			return;
		case TitanComponent.ALL_COMPREF:
			send_error(tc.socket, "Message CANCEL_DONE_ACK refers to 'all component'.");
			return;
		default:
			break;
		}
		final ComponentStruct started_tc = lookup_component(component_reference);

		if (started_tc == null) {
			send_error(tc.socket, MessageFormat.format("Message CANCEL_DONE_ACK refers to an invalid "
					+ "component reference: {0}.", component_reference));
			return;
		}
		done_cancelled(tc, started_tc);
		remove_requestor(tc.cancel_done_sent_for, started_tc);
	}

	private void process_is_alive(final ComponentStruct tc) {
		if (!request_allowed(tc, "IS_ALIVE")) {
			return;
		}

		final Text_Buf text_buf = tc.text_buf;
		final int component_reference = text_buf.pull_int().get_int();

		switch (component_reference) {
		case TitanComponent.NULL_COMPREF:
			send_error(tc.socket, "Alive operation was requested on the null component reference.");
			return;
		case TitanComponent.MTC_COMPREF:
			send_error(tc.socket, "Alive operation was requested on the component reference of the MTC.");
			return;
		case TitanComponent.SYSTEM_COMPREF:
			send_error(tc.socket, "Alive operation was requested on the component reference of the system.");
			return;
		case TitanComponent.ANY_COMPREF:
			if (tc.equals(mtc)) {
				send_alive(mtc, is_any_component_alive());
			} else {
				send_error(tc.socket, "Operation 'any component.alive' can only be performed on the MTC.");
			}
			return;
		case TitanComponent.ALL_COMPREF:
			if (tc.equals(mtc)) {
				send_alive(mtc, is_all_component_alive());
			} else {
				send_error(tc.socket, "Operation 'all component.alive' can only be performed on the MTC.");
			}
			return;
		default:
			break;
		}

		// the operation refers to a specific PTC
		final ComponentStruct comp = lookup_component(component_reference);
		if (comp == null) {
			send_error(tc.socket, MessageFormat.format("The argument of alive operation is an invalid "
					+ "component reference: {0}.", component_reference));
			return;
		}

		switch (comp.tc_state) {
		case TC_IDLE:
		case TC_CREATE:
		case TC_START:
		case TC_STOP:
		case TC_KILL:
		case TC_CONNECT:
		case TC_DISCONNECT:
		case TC_MAP:
		case TC_UNMAP:
		case TC_STOPPING:
		case PTC_FUNCTION:
		case PTC_STARTING:
		case PTC_STOPPED:
		case PTC_KILLING:
		case PTC_STOPPING_KILLING:
			send_alive(tc, true);
			break;
		case TC_EXITING:
		case TC_EXITED:
			send_alive(tc, false);
			break;
		case PTC_STALE:
			send_error(tc.socket, MessageFormat.format("The argument of alive operation ({0}) is a "
					+ "component reference that belongs to an earlier testcase.", component_reference));
			break;
		default:
			send_error(tc.socket, MessageFormat.format("The test component that the alive operation referes to "
					+ "({0}) is in invalid state.", component_reference));
		}
	}

	private boolean is_all_component_alive() {
		for (int i = tc_first_comp_ref; i < components.size(); i++) {
			final ComponentStruct comp = components.get(i);
			if (!component_is_alive(comp)) {
				return false;
			}
		}

		return true;
	}

	private void send_alive(final ComponentStruct tc, final boolean answer) {
		final Text_Buf text_buf = new Text_Buf();
		text_buf.push_int(MSG_ALIVE);
		text_buf.push_int(answer ? 1 : 0);
		send_message(tc.socket, text_buf);
	}

	private void process_is_running(final ComponentStruct tc) {
		if (!request_allowed(tc, "IS_RUNNING")) {
			return;
		}

		final Text_Buf text_buf = tc.text_buf;
		final int component_reference = text_buf.pull_int().get_int();

		switch (component_reference) {
		case TitanComponent.NULL_COMPREF:
			send_error(tc.socket, "Running operation was requested on the null component reference.");
			return;
		case TitanComponent.MTC_COMPREF:
			send_error(tc.socket, "Running operation was requested on the component reference of the MTC.");
			return;
		case TitanComponent.SYSTEM_COMPREF:
			send_error(tc.socket, "Running operation was requested on the component reference of the system.");
			return;
		case TitanComponent.ANY_COMPREF:
			if (tc.equals(mtc)) {
				send_running(mtc, is_any_component_running());
			} else {
				send_error(tc.socket, "Operation 'any component.running' can only be performed on the MTC.");
			}
			return;
		case TitanComponent.ALL_COMPREF:
			if (tc.equals(mtc)) {
				send_running(mtc, is_all_component_running());
			} else {
				send_error(tc.socket, "Operation 'all component.running' can only be performed on the MTC.");
			}
			return;
		default:
			break;
		}

		// the operation refers to a specific PTC
		final ComponentStruct comp = lookup_component(component_reference);
		if (comp == null) {
			send_error(tc.socket, MessageFormat.format("The argument of running operation is an invalid "
					+ "component reference: {0}.", component_reference));
			return;
		}

		switch (comp.tc_state) {
		case TC_CREATE:
		case TC_START:
		case TC_STOP:
		case TC_KILL:
		case TC_CONNECT:
		case TC_DISCONNECT:
		case TC_MAP:
		case TC_UNMAP:
		case TC_STOPPING:
		case PTC_FUNCTION:
		case PTC_STARTING:
		case PTC_STOPPING_KILLING:
			send_running(tc, true);
			break;
		case TC_IDLE:
		case TC_EXITING:
		case TC_EXITED:
		case PTC_STOPPED:
		case PTC_KILLING:
			send_running(tc, false);
			break;
		case PTC_STALE:
			send_error(tc.socket, MessageFormat.format("The argument of running operation ({0}) "
					+ "is a component reference that belongs to an earlier testcase.", component_reference));
			break;
		default:
			send_error(tc.socket, MessageFormat.format("The test component that the running operation refers "
					+ "to ({0}) is in invalid state.", component_reference));
		}
	}

	private boolean is_all_component_running() {
		for (int i = tc_first_comp_ref; i < components.size(); i++) {
			final ComponentStruct tc = components.get(i);
			if (tc.stop_requested) {
				continue;
			}

			switch (tc.tc_state) {
			case TC_EXITING:
			case TC_EXITED:
			case PTC_STOPPED:
				return false;
			default:
				break;
			}
		}
		return true;
	}

	private void send_running(final ComponentStruct tc, final boolean answer) {
		final Text_Buf text_buf = new Text_Buf();
		text_buf.push_int(MSG_RUNNING);
		text_buf.push_int(answer ? 1 : 0);
		send_message(tc.socket, text_buf);
	}

	private void process_mapped(final ComponentStruct tc) {
		if (!message_expected(tc, "MAPPED")) {
			return;
		}

		final Text_Buf text_buf = tc.text_buf;
		final boolean translation = text_buf.pull_int().get_int() == 0 ? false : true;
		final String localPort = text_buf.pull_string();
		final String systemPort = text_buf.pull_string();
		final int nof_params = text_buf.pull_int().get_int();

		final Map_Params params = new Map_Params(nof_params);
		for (int i = 0; i < nof_params; i++) {
			final String par = text_buf.pull_string();
			params.set_param(i, new TitanCharString(par));
		}

		PortConnection conn = null;
		if (!translation) {
			conn = find_connection(tc.comp_ref, localPort, TitanComponent.SYSTEM_COMPREF, systemPort);
		} else {
			conn = find_connection(TitanComponent.SYSTEM_COMPREF, localPort, tc.comp_ref, systemPort);
		}
		if (conn == null) {
			send_error(tc.socket, MessageFormat.format("The MAPPED message refers to a non-existent "
					+ "port mapping {0}:{1} - system:{2}.", tc.comp_ref, localPort, systemPort));
		} else if (conn.conn_state != conn_state_enum.CONN_MAPPING && conn.conn_state != conn_state_enum.CONN_MAPPED
				&& translation) {
			send_error(tc.socket, MessageFormat.format("Unexpected MAPPED message was received for mapping "
					+ "{0}:{1} - system:{2}.", tc.comp_ref, localPort, systemPort));
		} else {
			for (int i = 0;; i++) {
				final ComponentStruct comp = get_requestor(conn.requestors, i);
				if (comp == null) {
					break;
				}
				if (comp.tc_state == tc_state_enum.TC_MAP) {
					send_map_ack(comp, nof_params, params);
					if (comp.equals(mtc)) {
						comp.tc_state = tc_state_enum.MTC_TESTCASE;
					} else {
						comp.tc_state = tc_state_enum.PTC_FUNCTION;
					}
				}
			}
			free_requestors(conn.requestors);
			conn.conn_state = conn_state_enum.CONN_MAPPED;
			status_change();
		}
	}

	private void process_killed(final ComponentStruct tc) {
		// notify("Process killed: " + tc.tc_state.toString());
		switch (tc.tc_state) {
		case TC_IDLE:
		case PTC_STOPPED:
		case PTC_KILLING:
			break;
		default:
			send_error(tc.socket, "Unexpected message KILLED was received.");
			// also notify the user because the above message may get lost
			notify(MessageFormat.format("Unexpected message KILLED was received from PTC {0}.", tc.comp_ref));
			return;
		}

		final Text_Buf text_buf = tc.text_buf;
		tc.local_verdict = VerdictTypeEnum.values()[text_buf.pull_int().get_int()];
		tc.verdict_reason = text_buf.pull_string();

		// start a guard timer to detect whether the control connection is closed in time
		if (tc.tc_state != tc_state_enum.PTC_KILLING) {
			start_kill_timer(tc);
		}

		component_terminated(tc);
		status_change();
	}

	private void process_configure_nak_mtc() {
		if (mtc.tc_state != tc_state_enum.MTC_CONFIGURING) {
			send_error(mtc.socket, "Unexpected message CONFIGURE_NAK was received.");
			return;
		}
		mtc.tc_state = tc_state_enum.TC_IDLE;
		notify("Processing of configuration file failed on the MTC.");
	}

	private void process_configure_ack_mtc() {
		if (mtc.tc_state != tc_state_enum.MTC_CONFIGURING) {
			send_error(mtc.socket, "Unexpected message CONFIGURE_ACK was received.");
			return;
		}
		mtc.tc_state = tc_state_enum.TC_IDLE;
		notify("Configuration file was processed on the MTC.");
	}

	private void process_map_req(final ComponentStruct tc) {
		if (!request_allowed(tc, "MAP_REQ")) {
			return;
		}

		final Text_Buf text_buf = tc.text_buf;
		final int sourceComponent = text_buf.pull_int().get_int();
		final boolean translation = text_buf.pull_int().get_int() == 0 ? false : true;
		final String sourcePort = text_buf.pull_string();
		final String systemPort = text_buf.pull_string();
		final int nof_params = text_buf.pull_int().get_int();

		if (!valid_endpoint(sourceComponent, true, tc, "map")) {
			return;
		}

		final Map_Params params = new Map_Params(nof_params);
		for (int i = 0; i < nof_params; i++) {
			final String par = text_buf.pull_string();
			params.set_param(i, new TitanCharString(par));
		}

		PortConnection conn = find_connection(sourceComponent, sourcePort, TitanComponent.SYSTEM_COMPREF, systemPort);
		if (conn == null) {
			send_map(components.get(sourceComponent), sourcePort, systemPort, nof_params, params, translation);
			conn = new PortConnection();
			conn.headComp = sourceComponent;
			conn.headPort = sourcePort;
			conn.tailComp = TitanComponent.SYSTEM_COMPREF;
			conn.tailPort = systemPort;
			conn.requestors = init_requestors(tc);
			conn.conn_state = conn_state_enum.CONN_MAPPING;
			add_connection(conn);
			tc.tc_state = tc_state_enum.TC_MAP;
			status_change();
		} else {
			switch (conn.conn_state) {
			case CONN_MAPPING:
				add_requestor(conn.requestors, tc);
				tc.tc_state = tc_state_enum.TC_MAP;
				status_change();
				break;
			case CONN_MAPPED:
				send_map_ack(tc, nof_params, params);
				break;
			case CONN_UNMAPPING:
				send_error(tc.socket, MessageFormat.format("The port mapping {0}:{1} - system:{2} cannot be "
						+ "established because an unmap operation is in progress on it.", sourceComponent, sourcePort, systemPort));
				break;
			default:
				send_error(tc.socket, MessageFormat.format("The port mapping {0}:{1} - system:{2} "
						+ "is in invalid state.", sourceComponent, sourcePort, systemPort));
			}
		}
	}

	private void send_map_ack(final ComponentStruct tc, final int nof_params, final Map_Params params) {
		final Text_Buf text_buf = new Text_Buf();
		text_buf.push_int(MSG_MAP_ACK);
		text_buf.push_int(nof_params);
		for (int i = 0; i < nof_params; i++) {
			text_buf.push_string(params.get_param(i).get_value().toString());
		}

		send_message(tc.socket, text_buf);
	}

	private void send_map(final ComponentStruct tc, final String sourcePort, final String systemPort, final int nof_params,
			final Map_Params params, final boolean translation) {
		final Text_Buf text_buf = new Text_Buf();
		text_buf.push_int(MSG_MAP);
		text_buf.push_int(translation ? 1 : 0);
		text_buf.push_string(sourcePort);
		text_buf.push_string(systemPort);
		text_buf.push_int(nof_params);
		for (int i = 0; i < nof_params; i++) {
			text_buf.push_string(params.get_param(i).get_value().toString());
		}

		send_message(tc.socket, text_buf);
	}

	private void process_kill_req(final ComponentStruct tc) {
		if (!request_allowed(tc, "KILL_REQ")) {
			return;
		}

		final Text_Buf text_buf = tc.text_buf;
		final int component_reference = text_buf.pull_int().get_int();
		switch (component_reference) {
		case TitanComponent.NULL_COMPREF:
			send_error(tc.socket, "Kill operation was requested on the null component reference.");
			return;
		case TitanComponent.MTC_COMPREF:
			send_error(tc.socket, "Kill operation was requested on the component reference of the MTC.");
			return;
		case TitanComponent.SYSTEM_COMPREF:
			send_error(tc.socket, "Kill operation was requested on the component reference of the system.");
			return;
		case TitanComponent.ANY_COMPREF:
			send_error(tc.socket, "Kill operation was requested on 'any component'.");
			return;
		case TitanComponent.ALL_COMPREF:
			if (tc.equals(mtc)) {
				if (kill_all_components(false)) {
					send_kill_ack(mtc);
				} else {
					mtc.tc_state = tc_state_enum.MTC_ALL_COMPONENT_KILL;
					status_change();
				}
			} else {
				send_error(tc.socket, "Operation 'all component.kill' can only be performed on the MTC.");
			}
			return;
		default:
			break;
		}

		// the operation refers to a specific PTC
		final ComponentStruct target = lookup_component(component_reference);
		if (target == null) {
			send_error(tc.socket, MessageFormat.format("The argument of kill operation is an invalid "
					+ "component reference: {0}.", component_reference));
			return;
		} else if (target.equals(tc)) {
			send_error(tc.socket, "Kill operation was requested on the requestor component itself.");
			return;
		}

		boolean target_inactive = false;
		switch (target.tc_state) {
		case PTC_STOPPED:
			// the done status of this PTC is already sent out
			// and it will not be cancelled in the future
			free_requestors(target.done_requestors);
			// no break
		case TC_IDLE:
			target_inactive = true;
			// no break
		case TC_CREATE:
		case TC_START:
		case TC_STOP:
		case TC_KILL:
		case TC_CONNECT:
		case TC_DISCONNECT:
		case TC_MAP:
		case TC_UNMAP:
		case PTC_FUNCTION:
			send_kill(target);
			if (target_inactive) {
				// the PTC was inactive
				target.tc_state = tc_state_enum.PTC_KILLING;
				if (!target.is_alive) {
					target.stop_requested = true;
				}
			} else {
				// the PTC was active
				target.tc_state = tc_state_enum.PTC_STOPPING_KILLING;
				target.stop_requested = true;
			}

			target.stop_requestors = init_requestors(null);
			target.kill_requestors = init_requestors(tc);
			start_kill_timer(target);
			tc.tc_state = tc_state_enum.TC_KILL;
			status_change();
			break;
		case TC_STOPPING:
			// the PTC is currently being stopped
			send_kill(target);
			target.tc_state = tc_state_enum.PTC_STOPPING_KILLING;
			if (target.kill_timer != null) {
				cancel_timer(target.kill_timer);
			}

			start_kill_timer(target);
			// no break
		case PTC_KILLING:
		case PTC_STOPPING_KILLING:
			// the PTC is currently being terminated
			add_requestor(target.kill_requestors, tc);
			tc.tc_state = tc_state_enum.TC_KILL;
			status_change();
			break;
		case TC_EXITING:
		case TC_EXITED:
			// the PTC is already terminated
			send_kill_ack(tc);
			break;
		case PTC_STARTING:
			send_error(tc.socket, MessageFormat.format("PTC with component reference {0} cannot be killed "
					+ "because it is currently being started.", component_reference));
			break;
		case PTC_STALE:
			send_error(tc.socket, MessageFormat.format("The argument of kill operation ({0}) is a component "
					+ "reference that belongs to an earlier testcase.", component_reference));
			break;
		default:
			send_error(tc.socket, MessageFormat.format("The test component that the kill operation refers to "
					+ "({0}) is in invalid state.", component_reference));
		}
	}

	private void process_stopped_killed(final ComponentStruct tc, final int msg_end) {
		switch (tc.tc_state) {
		case TC_CREATE:
		case TC_START:
		case TC_STOP:
		case TC_KILL:
		case TC_CONNECT:
		case TC_DISCONNECT:
		case TC_MAP:
		case TC_UNMAP:
		case TC_STOPPING:
		case PTC_FUNCTION:
		case PTC_STOPPING_KILLING:
			break;
		default:
			send_error(tc.socket, "Unexpected message STOPPED_KILLED was received.");
			notify(MessageFormat.format("Unexpected message STOPPED_KILLED was received from PTC {0}.", tc.comp_ref));
			return;
		}

		final Text_Buf text_buf = tc.text_buf;
		tc.local_verdict = TitanVerdictType.VerdictTypeEnum.values()[text_buf.pull_int().get_int()];
		tc.verdict_reason = text_buf.pull_string();
		tc.return_type = text_buf.pull_string();

		tc.return_value = new byte[msg_end - text_buf.get_pos()];
		text_buf.pull_raw(tc.return_value.length, tc.return_value);

		// start a guard timer to detect whether the control connection is closed in time
		if (tc.tc_state != tc_state_enum.PTC_STOPPING_KILLING) {
			start_kill_timer(tc);
		}

		component_terminated(tc);
		status_change();
	}

	private void component_terminated(final ComponentStruct tc) {
		// the state variable of the PTC has to be updated first
		// because in case of 'all component.kill' or 'all component.stop'
		// we are walking through the states of all PTCs
		final tc_state_enum old_state = tc.tc_state;
		tc.tc_state = tc_state_enum.TC_EXITING;
		n_active_ptcs--;
		tc.comp_location.n_active_components--;
		switch (mc_state) {
		case MC_EXECUTING_TESTCASE:
			// this is the correct state
			break;
		case MC_TERMINATING_TESTCASE:
			// do nothing, we are waiting for the end of all PTC connections
			return;
		default:
			error(MessageFormat.format("PTC {0} terminated in invalid MC state.", tc.comp_ref));
			return;
		}

		// sending out COMPONENT_STATUS messages
		// Notes:
		// - the COMPONENT_STATUS message must be sent before STOP_ACK and KILL_ACK
		//   because the latter may update the component status cache table to an
		//   inconsistent state
		// - unsolicited 'done' status and return value is never sent out
		// the flags below indicate whether a COMPONENT_STATUS message
		// (with or without the return value) has to be sent to the MTC
		boolean send_status_to_mtc = false;
		boolean send_done_to_mtc = true;
		// first send out the COMPONENT_STATUS messages to PTCs
		for (int i = 0;; i++) {
			final ComponentStruct requestor = get_requestor(tc.done_requestors, i);
			if (requestor == null) {
				break;
			} else if (requestor.equals(mtc)) {
				send_status_to_mtc = true;
				send_done_to_mtc = true;
			} else {
				send_component_status_to_requestor(tc, requestor, true, true);
			}
		}
		for (int i = 0;; i++) {
			final ComponentStruct requestor = get_requestor(tc.killed_requestors, i);
			if (requestor == null) {
				break;
			} else if (requestor.equals(mtc)) {
				send_status_to_mtc = true;
			} else if (!has_requestor(tc.done_requestors, requestor)) {
				send_component_status_to_requestor(tc, requestor, false, true);
			}
		}

		free_requestors(tc.done_requestors);
		free_requestors(tc.killed_requestors);

		// deciding whether to send a COMPONENT_STATUS message to MTC
		// 'any component.done' status can be safely sent out
		// it will not be cancelled later
		if (any_component_done_requested || any_component_killed_requested) {
			send_status_to_mtc = true;
		}
		boolean all_done_checked = false;
		boolean all_done_result = false;
		if (all_component_done_requested) {
			all_done_checked = true;
			all_done_result = !is_any_component_running();
			if (all_done_result) {
				send_status_to_mtc = true;
			}
		}
		boolean all_killed_checked = false;
		boolean all_killed_result = false;
		if (all_component_killed_requested) {
			all_killed_checked = true;
			all_killed_result = !is_any_component_alive();
			if (all_killed_result) {
				send_status_to_mtc = true;
			}
		}

		// sending the COMPONENT_STATUS message to MTC if necessary
		if (send_status_to_mtc) {
			if (!all_done_checked) {
				all_done_result = !is_any_component_running();
			}
			if (!all_killed_checked) {
				all_killed_result = !is_any_component_alive();
			}
			if (send_done_to_mtc) {
				// the return value was requested
				send_component_status_mtc(tc.comp_ref, true, true, true, all_done_result, true, all_killed_result,
						tc.local_verdict, tc.return_type, tc.return_value);
			} else {
				send_component_status_mtc(tc.comp_ref, false, true, true, all_done_result, true, all_killed_result,
						VerdictTypeEnum.NONE, null, null);
			}
			any_component_done_requested = false;
			any_component_done_sent = true;
			any_component_killed_requested = false;
			if (all_done_result) {
				all_component_done_requested = false;
			}
			if (all_killed_result) {
				all_component_killed_requested = false;
			}
		}

		// sending out STOP_ACK and KILL_ACK messages if necessary
		switch (old_state) {
		case TC_STOPPING:
		case PTC_STOPPING_KILLING:
		case PTC_KILLING:
			// the component was explicitly stopped and/or killed
			if (mtc.tc_state == tc_state_enum.MTC_ALL_COMPONENT_KILL) {
				check_all_component_kill();
			} else if (mtc.tc_state == tc_state_enum.MTC_ALL_COMPONENT_STOP) {
				check_all_component_stop();
			} else {
				send_stop_ack_to_requestors(tc);
				send_kill_ack_to_requestors(tc);
			}
		default:
			break;
		}

		// we should behave as we got all pending CANCEL_DONE_ACK messages from tc
		for (int i = 0;; i++) {
			final ComponentStruct comp = get_requestor(tc.cancel_done_sent_for, i);
			if (comp == null) {
				break;
			}
			done_cancelled(tc, comp);
		}
		free_requestors(tc.cancel_done_sent_for);

		// destroy all connections and mappings of the component
		// and send out the related messages
		Iterator<PortConnection> it = tc.conn_head_list.iterator();
		while (it.hasNext()) {
			final PortConnection conn = it.next();
			if (conn.tailComp == TitanComponent.SYSTEM_COMPREF) {
				destroy_mapping(conn, 0, null, it);
			} else {
				destroy_connection(conn, tc, it);
			}
			tc.conn_tail_list.remove(conn);
		}
		it = tc.conn_tail_list.iterator();
		while (it.hasNext()) {
			final PortConnection conn = it.next();
			if (conn.headComp == TitanComponent.SYSTEM_COMPREF) {
				destroy_mapping(conn, 0, null, it);
			} else {
				destroy_connection(conn, tc, it);
			}
			tc.conn_head_list.remove(conn);
		}

		tc.tc_fn_name = new QualifiedName("", "");

	}

	private void destroy_connection(final PortConnection conn, final ComponentStruct tc, final Iterator<PortConnection> it) {
		switch (conn.conn_state) {
		case CONN_LISTENING:
		case CONN_CONNECTING:
			if (conn.transport_type != transport_type_enum.TRANSPORT_LOCAL &&
			conn.headComp != tc.comp_ref) {
				// shut down the server side if the client has terminated
				send_disconnect_to_server(conn);
			}
			send_error_to_connect_requestors(conn, "test component " + tc.comp_ref + " has terminated during connection setup.");

			break;
		case CONN_CONNECTED:
			break;
		case CONN_DISCONNECTING:
			send_disconnect_ack_to_requestors(conn);
			break;
		default:
			error(MessageFormat.format("The port connection {0}:{1} - {2}:{3} is in invalid state when test component {4} has terminated.",
					conn.headComp, conn.headPort, conn.tailComp, conn.tailPort, tc.comp_ref));
		}
		it.remove();
	}

	private void destroy_mapping(final PortConnection conn, final int nof_params, final Map_Params params,
			final Iterator<PortConnection> it) {
		int tc_compref;
		String tc_port;
		String system_port;
		if (conn.headComp == TitanComponent.SYSTEM_COMPREF) {
			tc_compref = conn.tailComp;
			tc_port = conn.tailPort;
			system_port = conn.headPort;
		} else {
			tc_compref = conn.headComp;
			tc_port = conn.headPort;
			system_port = conn.tailPort;
		}

		switch (conn.conn_state) {
		case CONN_UNMAPPING:
			for (int i = 0;; i++) {
				final ComponentStruct comp = get_requestor(conn.requestors, i);
				if (comp == null) {
					break;
				}
				if (comp.tc_state == tc_state_enum.TC_UNMAP) {
					send_unmap_ack(comp, nof_params, params);
					if (comp.equals(mtc)) {
						comp.tc_state = tc_state_enum.MTC_TESTCASE;
					} else {
						comp.tc_state = tc_state_enum.PTC_FUNCTION;
					}
				}
			}
			break;
		case CONN_MAPPING:
			for (int i = 0;; i++) {
				final ComponentStruct comp = get_requestor(conn.requestors, i);
				if (comp == null) {
					break;
				}
				if (comp.tc_state == tc_state_enum.TC_MAP) {
					send_error(comp.socket, MessageFormat.format("Establishment of port mapping {0}:{1} - "
							+ "system:{2} failed because the test component endpoint has terminated.", tc_compref, tc_port, system_port));
					if (comp.equals(mtc)) {
						comp.tc_state = tc_state_enum.MTC_TESTCASE;
					} else {
						comp.tc_state = tc_state_enum.PTC_FUNCTION;
					}
				}
			}
		default:
			break;
		}
		if (it != null) {
			it.remove();
		} else {
			remove_connection(conn);
		}
	}

	private void send_unmap_ack(final ComponentStruct tc, final int nof_params, final Map_Params params) {
		final Text_Buf text_buf = new Text_Buf();
		text_buf.push_int(MSG_UNMAP_ACK);
		text_buf.push_int(nof_params);
		for (int i = 0; i < nof_params; i++) {
			text_buf.push_string(params.get_param(i).get_value().toString());
		}
		send_message(tc.socket, text_buf);

	}

	private void done_cancelled(final ComponentStruct from, final ComponentStruct started_tc) {
		if (started_tc.tc_state != tc_state_enum.PTC_STARTING) {
			return;
		}
		remove_requestor(started_tc.cancel_done_sent_to, from);
		if (get_requestor(started_tc.cancel_done_sent_to, 0) != null) {
			return;
		}

		send_start(started_tc, started_tc.tc_fn_name, started_tc.arg);
		final ComponentStruct start_requestor = started_tc.start_requestor;
		if (start_requestor.tc_state == tc_state_enum.TC_START) {
			send_start_ack(start_requestor);
			if (start_requestor.equals(mtc)) {
				start_requestor.tc_state = tc_state_enum.MTC_TESTCASE;
			} else {
				start_requestor.tc_state = tc_state_enum.PTC_FUNCTION;
			}
		}

		started_tc.arg = null;
		free_requestors(started_tc.cancel_done_sent_to);
		started_tc.tc_state = tc_state_enum.PTC_FUNCTION;
		status_change();
	}

	private void remove_requestor(final RequestorStruct reqs, final ComponentStruct tc) {
		reqs.components.remove(tc);
	}

	private void send_kill_ack_to_requestors(final ComponentStruct tc) {
		for (int i = 0;; i++) {
			final ComponentStruct requestor = get_requestor(tc.kill_requestors, i);
			if (requestor == null) {
				break;
			}
			if (requestor.tc_state == tc_state_enum.TC_KILL) {
				send_kill_ack(requestor);
				if (requestor.equals(mtc)) {
					requestor.tc_state = tc_state_enum.MTC_TESTCASE;
				} else {
					requestor.tc_state = tc_state_enum.PTC_FUNCTION;
				}
			}
		}
		free_requestors(tc.kill_requestors);
	}

	private void send_stop_ack_to_requestors(final ComponentStruct tc) {
		for (int i = 0;; i++) {
			final ComponentStruct requestor = get_requestor(tc.stop_requestors, i);
			if (requestor == null) {
				break;
			}
			if (requestor.tc_state == tc_state_enum.TC_STOP) {
				send_stop_ack(requestor);
				if (requestor.equals(mtc)) {
					requestor.tc_state = tc_state_enum.MTC_TESTCASE;
				} else {
					requestor.tc_state = tc_state_enum.PTC_FUNCTION;
				}
			}
		}
		free_requestors(tc.stop_requestors);
	}

	private void check_all_component_stop() {
		boolean ready_for_ack = true;
		for (int i = tc_first_comp_ref; i < components.size(); i++) {
			final ComponentStruct comp = components.get(i);

			switch (comp.tc_state) {
			case TC_INITIAL:
			case PTC_KILLING:
				if (!comp.is_alive) {
					ready_for_ack = false;
				}
				break;
			case TC_STOPPING:
			case PTC_STOPPING_KILLING:
				ready_for_ack = false;
				break;
			case TC_EXITING:
			case TC_EXITED:
			case PTC_STOPPED:
			case PTC_STALE:
				break;
			case TC_IDLE:
				// only alive components can be in idle state
				if (comp.is_alive) {
					break;
				}
			default:
				error(MessageFormat.format("PTC {0} is in invalid state when performing " +
						"'all component.stop' operation.", comp.comp_ref));
			}

			if (!ready_for_ack) {
				break;
			}
		}

		if (ready_for_ack) {
			send_stop_ack(mtc);
			mtc.tc_state = tc_state_enum.MTC_TESTCASE;
		}
	}

	private void check_all_component_kill() {
		// MTC has requested 'all component.kill'
		// we have to send acknowledgement to MTC only
		boolean ready_for_ack = true;
		for (int i = tc_first_comp_ref; i < components.size(); i++) {
			final ComponentStruct comp = components.get(i);
			switch (comp.tc_state) {
			case TC_INITIAL:
			case PTC_STOPPING_KILLING:
			case PTC_KILLING:
				ready_for_ack = false;
			case TC_EXITING:
			case TC_EXITED:
			case PTC_STALE:
				break;
			default:
				error(MessageFormat.format("PTC %d is in invalid state when performing 'all component.kill' operation.", comp.comp_ref));
			}
			if (!ready_for_ack) {
				break;
			}
		}
		if (ready_for_ack) {
			send_kill_ack(mtc);
			mtc.tc_state = tc_state_enum.MTC_TESTCASE;
		}
	}

	private void send_kill_ack(final ComponentStruct tc) {
		final Text_Buf text_buf = new Text_Buf();
		text_buf.push_int(MSG_KILL_ACK);
		send_message(tc.socket, text_buf);
	}

	private void send_component_status_mtc(final int comp_ref, final boolean is_done, final boolean is_killed, final boolean is_any_done,
			final boolean is_all_done, final boolean is_any_killed, final boolean is_all_killed, final VerdictTypeEnum local_verdict,
			final String return_type, final byte[] return_value) {

		final Text_Buf text_buf = new Text_Buf();
		text_buf.push_int(MSG_COMPONENT_STATUS);
		text_buf.push_int(comp_ref);
		text_buf.push_int(is_done ? 1 : 0);
		text_buf.push_int(is_killed ? 1 : 0);
		text_buf.push_int(is_any_done ? 1 : 0);
		text_buf.push_int(is_all_done ? 1 : 0);
		text_buf.push_int(is_any_killed ? 1 : 0);
		text_buf.push_int(is_all_killed ? 1 : 0);
		text_buf.push_int(local_verdict.getValue());
		text_buf.push_string(return_type);

		if (return_value != null) {
			text_buf.push_raw(return_value);
		}

		send_message(mtc.socket, text_buf);
	}

	private boolean is_any_component_alive() {
		for (int i = tc_first_comp_ref; i < components.size(); i++) {
			final ComponentStruct comp = components.get(i);
			if (component_is_alive(comp)) {
				return true;
			}
		}

		return false;
	}

	private boolean component_is_alive(final ComponentStruct tc) {
		switch (tc.tc_state) {
		case TC_INITIAL:
		case TC_IDLE:
		case TC_CREATE:
		case TC_START:
		case TC_STOP:
		case TC_KILL:
		case TC_CONNECT:
		case TC_DISCONNECT:
		case TC_MAP:
		case TC_UNMAP:
		case TC_STOPPING:
		case PTC_FUNCTION:
		case PTC_STARTING:
		case PTC_STOPPED:
		case PTC_KILLING:
		case PTC_STOPPING_KILLING:
			return true;
		case TC_EXITING:
		case TC_EXITED:
			return false;
		default:
			error(MessageFormat.format("PTC {0} is in invalid state when checking whether it is alive.", tc.comp_ref));
			return false;
		}
	}

	private boolean is_any_component_running() {
		for (int i = tc_first_comp_ref; i < components.size(); i++) {
			final ComponentStruct comp = components.get(i);
			if (component_is_running(comp)) {
				return true;
			}
		}

		return false;
	}

	private boolean component_is_running(final ComponentStruct tc) {
		switch (tc.tc_state) {
		case TC_CREATE:
		case TC_START:
		case TC_STOP:
		case TC_KILL:
		case TC_CONNECT:
		case TC_DISCONNECT:
		case TC_MAP:
		case TC_UNMAP:
		case TC_STOPPING:
		case PTC_FUNCTION:
		case PTC_STARTING:
		case PTC_STOPPING_KILLING:
			return true;
		case TC_INITIAL:
		case TC_IDLE:
		case TC_EXITING:
		case TC_EXITED:
		case PTC_STOPPED:
		case PTC_KILLING:
			return false;
		default:
			error(MessageFormat.format("PTC {0} is in invalid state when checking whether it is running.", tc.comp_ref));
			return false;
		}
	}

	private void send_component_status_to_requestor(final ComponentStruct tc, final ComponentStruct requestor, final boolean done_status,
			final boolean killed_status) {
		switch (requestor.tc_state) {
		case PTC_FUNCTION:
		case TC_CREATE:
		case TC_START:
		case TC_STOP:
		case TC_KILL:
		case TC_CONNECT:
		case TC_DISCONNECT:
		case TC_MAP:
		case TC_UNMAP:
		case TC_STOPPING:
		case PTC_STOPPED:
		case PTC_STARTING:
			if (done_status) {
				send_component_status_ptc(requestor, tc.comp_ref, true, killed_status, tc.local_verdict,
						tc.return_type, tc.return_value);
			} else {
				send_component_status_ptc(requestor, tc.comp_ref, false, killed_status, tc.local_verdict, null, null);
			}
			break;
		case PTC_STOPPING_KILLING:
		case PTC_KILLING:
		case TC_EXITING:
		case TC_EXITED:
			// the PTC requestor is not interested in the component status anymore
			break;
		default:
			error(MessageFormat.format("PTC {0} is in invalid state when sending out COMPONENT_STATUS message about PTC {1}.",
					requestor.comp_ref, tc.comp_ref));
		}
	}

	private void send_component_status_ptc(final ComponentStruct tc, final int comp_ref, final boolean is_done, final boolean is_killed,
			final VerdictTypeEnum local_verdict, final String return_type, final byte[] return_value) {
		final Text_Buf text_buf = new Text_Buf();
		text_buf.push_int(MSG_COMPONENT_STATUS);
		text_buf.push_int(comp_ref);
		text_buf.push_int(is_done ? 1 : 0);
		text_buf.push_int(is_killed ? 1 : 0);
		text_buf.push_int(local_verdict.getValue());
		text_buf.push_string(return_type);
		if (tc.return_value != null) {
			text_buf.push_raw(tc.return_value);
		}
		send_message(tc.socket, text_buf);
	}

	private void process_stop_req(final ComponentStruct tc) {
		if (!request_allowed(tc, "STOP_REQ")) {
			return;
		}

		final Text_Buf text_buf = tc.text_buf;
		final int component_reference = text_buf.pull_int().get_int();

		switch (component_reference) {
		case TitanComponent.NULL_COMPREF:
			send_error(tc.socket, "Stop operation was requested on the null component reference.");
			return;
		case TitanComponent.MTC_COMPREF:
			// 'mtc.stop' initiated by a PTC terminates the current testcase
			if (!tc.equals(mtc)) {
				if (!mtc.stop_requested) {
					send_stop(mtc);
					kill_all_components(true);
					mtc.stop_requested = true;
					start_kill_timer(mtc);
					notify(MessageFormat.format("Test Component {0} had requested to stop MTC. Terminating current testcase execution.", tc.comp_ref));
					status_change();
				}
			} else {
				send_error(tc.socket, "MTC has requested to stop itself.");
			}
			return;
		case TitanComponent.SYSTEM_COMPREF:
			send_error(tc.socket, "Stop operation was requested on the component reference of the system.");
			return;
		case TitanComponent.ANY_COMPREF:
			send_error(tc.socket, "Stop operation was requested on 'any component'.");
			return;
		case TitanComponent.ALL_COMPREF:
			if (tc.equals(mtc)) {
				if (stop_all_components()) {
					send_stop_ack(mtc);
				} else {
					mtc.tc_state = tc_state_enum.MTC_ALL_COMPONENT_STOP;
					status_change();
				}
			} else {
				send_error(tc.socket, "Operation 'all component.stop' can only be performed on the MTC.");
			}
			return;
		default:
			break;
		}

		// the operation refers to a specific PTC
		final ComponentStruct target = lookup_component(component_reference);
		if (target == null) {
			send_error(tc.socket, MessageFormat.format("The argument of stop operation is an invalid component reference {0}.", component_reference));
			return;
		} else if (target.equals(tc)) {
			send_error(tc.socket, "Stop operation was requested on the requestor component itself.");
			return;
		}
		boolean target_inactive = false;
		switch (target.tc_state) {
		case PTC_STOPPED:
			if (!target.is_alive) {
				error( MessageFormat.format("PTC {0} cannot be in state STOPPED because it is not an "
						+ "alive type PTC.", component_reference));
			}
			// no break
		case TC_IDLE:
			target_inactive = true;
			// no break
		case TC_CREATE:
		case TC_START:
		case TC_STOP:
		case TC_KILL:
		case TC_CONNECT:
		case TC_DISCONNECT:
		case TC_MAP:
		case TC_UNMAP:
		case PTC_FUNCTION:
			if (target.is_alive) {
				if (target_inactive) {
					// do nothing, just send a STOP_ACK to tc
					send_stop_ack(tc);
					break;
				} else {
					send_stop(target);
					target.tc_state = tc_state_enum.TC_STOPPING;
				}
			} else {
				// the target is not an alive type PTC: stop operation means kill
				send_kill(target);
				if (target_inactive) {
					target.tc_state = tc_state_enum.PTC_KILLING;
				} else {
					target.tc_state = tc_state_enum.PTC_STOPPING_KILLING;
				}
			}

			// a STOP or KILL message was sent out
			target.stop_requested = true;
			target.stop_requestors = init_requestors(tc);
			target.kill_requestors = init_requestors(null);
			start_kill_timer(target);
			tc.tc_state = tc_state_enum.TC_STOP;
			status_change();
			break;
		case PTC_KILLING:
			if (target.is_alive) {
				// do nothing if the PTC is alive
				send_stop_ack(tc);
				break;
			}
			// no break
		case TC_STOPPING:
		case PTC_STOPPING_KILLING:
			// the PTC is currently being stopped
			add_requestor(target.stop_requestors, tc);
			tc.tc_state = tc_state_enum.TC_STOP;
			status_change();
			break;
		case TC_EXITING:
		case TC_EXITED:
			// the PTC is already terminated, do nothing
			send_stop_ack(tc);
			break;
		case PTC_STARTING:
			send_error(tc.socket, MessageFormat.format("PTC with component reference {0} cannot be stopped "
					+ "because it is currently being started.", component_reference));
			break;
		case PTC_STALE:
			send_error(tc.socket, MessageFormat.format("The argument of stop operation ({0}) is a component "
					+ "reference that belongs to an earlier testcase.", component_reference));
			break;
		default:
			send_error(tc.socket, MessageFormat.format("The test component that the stop operation refers to "
					+ "({0}) is in invalid state.", component_reference));
		}
	}

	private void send_stop_ack(final ComponentStruct tc) {
		final Text_Buf text_buf = new Text_Buf();
		text_buf.push_int(MSG_STOP_ACK);
		send_message(tc.socket, text_buf);
	}

	private boolean stop_all_components() {
		boolean ready_for_ack = true;
		for (int i = tc_first_comp_ref; i < components.size(); i++) {
			final ComponentStruct tc = components.get(i);
			switch (tc.tc_state) {
			case TC_INITIAL:
				// we do not have to termiate the PTC (and wait for the control
				// connection) if it is alive
				if (!tc.is_alive) {
					ready_for_ack = false;
				}
				break;
			case TC_IDLE:
				// do nothing if the component is alive
				if (!tc.is_alive) {
					send_kill(tc);
					tc.tc_state = tc_state_enum.PTC_KILLING;
					tc.stop_requested = true;
					tc.stop_requestors = init_requestors(null);
					tc.kill_requestors = init_requestors(null);
					start_kill_timer(tc);
					ready_for_ack = false;
				}
				break;
			case TC_CREATE:
			case TC_START:
			case TC_STOP:
			case TC_KILL:
			case TC_CONNECT:
			case TC_DISCONNECT:
			case TC_MAP:
			case TC_UNMAP:
			case PTC_FUNCTION:
				// the PTC is executing behaviour
				if (tc.is_alive) {
					send_stop(tc);
					tc.tc_state = tc_state_enum.TC_STOPPING;
				} else {
					send_kill(tc);
					tc.tc_state = tc_state_enum.PTC_STOPPING_KILLING;
				}

				tc.stop_requested = true;
				tc.stop_requestors = init_requestors(null);
				tc.kill_requestors = init_requestors(null);
				start_kill_timer(tc);
				ready_for_ack = false;
				break;
			case PTC_STARTING:
				// do nothing, just put it back to STOPPED state
				tc.tc_fn_name = new QualifiedName("", "");
				tc.arg = null;
				free_requestors(tc.cancel_done_sent_to);
				tc.tc_state = tc_state_enum.PTC_STOPPED;
				break;
			case TC_STOPPING:
			case PTC_STOPPING_KILLING:
				free_requestors(tc.stop_requestors);
				free_requestors(tc.kill_requestors);
				ready_for_ack = false;
				break;
			case PTC_KILLING:
				free_requestors(tc.stop_requestors);
				free_requestors(tc.kill_requestors);
				// we have to wait only if the PTC is non-alive
				if (!tc.is_alive) {
					ready_for_ack = false;
				}
				break;
			case PTC_STOPPED:
			case TC_EXITING:
			case TC_EXITED:
			case PTC_STALE:
				break;
			default:
				error(MessageFormat.format("Test Component {0} is in invalid state when stopping all components.", tc.comp_ref));

			}

			// only mtc is preserved in done_requestors and killed_requestors
			final boolean mtc_requested_done = has_requestor(tc.done_requestors, mtc);
			free_requestors(tc.done_requestors);
			if (mtc_requested_done) {
				add_requestor(tc.done_requestors, mtc);
			}

			final boolean mtc_requested_killed = has_requestor(tc.killed_requestors, mtc);
			free_requestors(tc.kill_requestors);
			if (mtc_requested_killed) {
				add_requestor(tc.killed_requestors, mtc);
			}

			free_requestors(tc.cancel_done_sent_for);
		}

		return ready_for_ack;
	}

	private boolean kill_all_components(final boolean testcase_ends) {
		boolean ready_for_ack = true;
		for (int i = tc_first_comp_ref; i < components.size(); i++) {
			final ComponentStruct tc = components.get(i);
			boolean is_inactive = false;
			switch (tc.tc_state) {
			case TC_INITIAL:
				// the PTC does not have an identified control connection yet
				ready_for_ack = false;
				break;
			case PTC_STARTING:
				tc.tc_fn_name =  new QualifiedName("", "");
				tc.arg = null;
				free_requestors(tc.cancel_done_sent_to);
				// no break
			case TC_IDLE:
			case PTC_STOPPED:
				is_inactive = true;
				// no break
			case TC_CREATE:
			case TC_START:
			case TC_STOP:
			case TC_KILL:
			case TC_CONNECT:
			case TC_DISCONNECT:
			case TC_MAP:
			case TC_UNMAP:
			case PTC_FUNCTION:
				send_kill(tc);
				if (is_inactive) {
					// the PTC was inactive
					tc.tc_state = tc_state_enum.PTC_KILLING;
					if (!tc.is_alive) {
						tc.stop_requested = true;
					}
				} else {
					// the PTC was active
					tc.tc_state = tc_state_enum.PTC_STOPPING_KILLING;
					tc.stop_requested = true;
				}

				tc.stop_requestors = init_requestors(null);
				tc.kill_requestors = init_requestors(null);
				start_kill_timer(tc);
				ready_for_ack = false;
				break;
			case TC_STOPPING:
				send_kill(tc);
				tc.tc_state = tc_state_enum.PTC_STOPPING_KILLING;
				if (tc.kill_timer != null) {
					cancel_timer(tc.kill_timer);
				}

				start_kill_timer(tc);
				// no break
			case PTC_KILLING:
			case PTC_STOPPING_KILLING:
				free_requestors(tc.stop_requestors);
				free_requestors(tc.kill_requestors);
				ready_for_ack = false;
				break;
			case TC_EXITING:
				if (testcase_ends) {
					ready_for_ack = false;
				}
			case TC_EXITED:
			case PTC_STALE:
				break;
			default:
				error(MessageFormat.format("Test Component {0} is in invalid state when killing all components.", tc.comp_ref));
			}
			if (testcase_ends) {
				free_requestors(tc.done_requestors);
				free_requestors(tc.killed_requestors);
			} else {
				final boolean mtc_requested_done = has_requestor(tc.done_requestors, mtc);
				free_requestors(tc.done_requestors);
				if (mtc_requested_done) {
					add_requestor(tc.done_requestors, mtc);
				}

				final boolean mtc_requested_killed = has_requestor(tc.killed_requestors, mtc);
				free_requestors(tc.killed_requestors);
				if (mtc_requested_killed) {
					add_requestor(tc.killed_requestors, mtc);
				}
			}

			free_requestors(tc.cancel_done_sent_for);
		}
		return ready_for_ack;
	}

	private static boolean has_requestor(final RequestorStruct reqs, final ComponentStruct tc) {
		return reqs.components.contains(tc);
	}

	private void send_stop(final ComponentStruct tc) {
		final Text_Buf text_buf = new Text_Buf();
		text_buf.push_int(MSG_STOP);
		send_message(tc.socket, text_buf);
	}

	private void process_disconnected(final ComponentStruct tc) {
		if (!message_expected(tc, "DISCONNECTED")) {
			return;
		}

		final Text_Buf text_buf = tc.text_buf;
		final String sourcePort = text_buf.pull_string();
		final int remoteComponent = text_buf.pull_int().get_int();
		final String remotePort = text_buf.pull_string();

		final PortConnection conn = find_connection(tc.comp_ref, sourcePort, remoteComponent, remotePort);
		if (conn != null) {
			switch (conn.conn_state) {
			case CONN_LISTENING:
				// in this state only the server side (head) can report the end of the connection
				if (conn.headComp != tc.comp_ref || !conn.headPort.equals(sourcePort)) {
					send_error(tc.socket, MessageFormat.format("Unexpected message DISCONNECTED was received "
							+ "for port connection {0}:{1} - {2}:{3}.", tc.comp_ref, sourcePort, remoteComponent, remotePort));
					break;
				}
				// no break
			case CONN_CONNECTING:
				// in this state both ends can report the end of the connection
				send_error_to_connect_requestors(conn, "test component " + tc.comp_ref + " reported end of the connection during connection setup.");
				remove_connection(conn);
				status_change();
				break;
			case CONN_CONNECTED:
				remove_connection(conn);
				status_change();
				break;
			case CONN_DISCONNECTING:
				send_disconnect_ack_to_requestors(conn);
				remove_connection(conn);
				status_change();
				break;
			default:
				error(MessageFormat.format("The port connection {0}:{1} - {2}:{3} is in invalid state when MC was notified about its termination.", tc.comp_ref, sourcePort, remoteComponent, remotePort));
			}
		}

		status_change();
	}

	private void send_disconnect_ack_to_requestors(final PortConnection conn) {
		for (int i = 0;; i++) {
			final ComponentStruct comp = get_requestor(conn.requestors, i);
			if (comp == null) {
				break;
			} else if (comp.tc_state == tc_state_enum.TC_DISCONNECT) {
				send_disconnect_ack(comp);
				if (comp.equals(mtc)) {
					comp.tc_state = tc_state_enum.MTC_TESTCASE;
				} else {
					comp.tc_state = tc_state_enum.PTC_FUNCTION;
				}
			}
		}

		free_requestors(conn.requestors);
	}

	private void process_disconnect_req(final ComponentStruct tc) {
		if (!request_allowed(tc, "DISCONNECT_REQ")) {
			return;
		}

		final Text_Buf text_buf = tc.text_buf;
		final int sourceComponent = text_buf.pull_int().get_int();
		final String sourcePort = text_buf.pull_string();
		final int destinationComponent = text_buf.pull_int().get_int();
		final String destinationPort = text_buf.pull_string();

		if (!valid_endpoint(sourceComponent, false, tc, "disconnect") ||
				!valid_endpoint(destinationComponent, false, tc, "disconnect")) {
			return;
		}

		final PortConnection conn = find_connection(sourceComponent, sourcePort, destinationComponent, destinationPort);
		if (conn != null) {
			switch (conn.conn_state) {
			case CONN_LISTENING:
			case CONN_CONNECTING:
				send_error(tc.socket, MessageFormat.format("The port connection {0}:{1} - {2}:{3} cannot be destroyed because a connect operation is in progress on it.",
						sourceComponent, sourcePort, destinationComponent, destinationPort));
				break;
			case CONN_CONNECTED:
				send_disconnect(components.get(conn.tailComp), conn.tailPort, conn.headComp, conn.headPort);
				conn.conn_state = conn_state_enum.CONN_DISCONNECTING;
				// no break
			case CONN_DISCONNECTING:
				add_requestor(conn.requestors, tc);
				tc.tc_state = tc_state_enum.TC_DISCONNECT;
				status_change();
				break;
			default:
				send_error(tc.socket, MessageFormat.format("The port connection {0}:{1} - {2}:{3} cannot be destroyed due to an internal error in the MC.",
						sourceComponent, sourcePort, destinationComponent, destinationPort));
				error(MessageFormat.format("The port connection {0}:{1} - {2}:{3} is in invalid state when a disconnect operation was requested on it.",
						sourcePort, destinationComponent, destinationPort));
			}
		} else {
			// the connection is already terminated
			// send the acknowledgement immediately
			send_disconnect_ack(tc);
		}
	}

	private void send_disconnect_ack(final ComponentStruct tc) {
		final Text_Buf text_buf = new Text_Buf();
		text_buf.push_int(MSG_DISCONNECT_ACK);
		send_message(tc.socket, text_buf);
	}

	private void send_disconnect(final ComponentStruct tc, final String tailPort, final int headComp, final String headPort) {
		final Text_Buf text_buf = new Text_Buf();
		text_buf.push_int(MSG_DISCONNECT);
		text_buf.push_string(tailPort);
		text_buf.push_int(headComp);
		text_buf.push_string(headPort);

		send_message(tc.socket, text_buf);
	}

	private void process_start_req(final ComponentStruct tc) {
		if (!request_allowed(tc, "START_REQ")) {
			return;
		}

		final Text_Buf text_buf = tc.text_buf;
		final int component_reference = text_buf.pull_int().get_int();
		final String module_name = text_buf.pull_string();
		final String function_name = text_buf.pull_string();

		switch (component_reference) {
		case TitanComponent.NULL_COMPREF:
			send_error(tc.socket, "Start operation was requested on the null component reference.");
			return;
		case TitanComponent.MTC_COMPREF:
			send_error(tc.socket, "Start operation was requested on the component reference of the MTC.");
			return;
		case TitanComponent.SYSTEM_COMPREF:
			send_error(tc.socket, "Start operation was requested on the component reference of the system.");
			return;
		case TitanComponent.ANY_COMPREF:
			send_error(tc.socket, "Start operation was requested on 'any component'.");
			return;
		case TitanComponent.ALL_COMPREF:
			send_error(tc.socket, "Start operation was requested on 'all component'.");
			return;
		}

		final ComponentStruct target = lookup_component(component_reference);
		if (target == null) {
			send_error(tc.socket, MessageFormat.format("Start operation was requested on invalid component reference: {0}.",
					component_reference));
			return;
		}
		switch (target.tc_state) {
		case TC_IDLE:
		case PTC_STOPPED:
			// these states are correct
			break;
		case TC_CREATE:
		case TC_START:
		case TC_STOP:
		case TC_KILL:
		case TC_CONNECT:
		case TC_DISCONNECT:
		case TC_MAP:
		case TC_UNMAP:
		case PTC_FUNCTION:
		case PTC_STARTING:
			send_error(tc.socket, MessageFormat.format("PTC with component reference {0} cannot be started because it is already executing function {1}.{2}.",
					component_reference, target.tc_fn_name.module_name, target.tc_fn_name.definition_name));
			return;
		case TC_STOPPING:
			send_error(tc.socket, MessageFormat.format("PTC with component reference {0} cannot be started because its function {1}.{2} is currently being stopped on it.",
					component_reference, target.tc_fn_name.module_name, target.tc_fn_name.definition_name));
			return;
		case PTC_KILLING:
		case PTC_STOPPING_KILLING:
			send_error(tc.socket, MessageFormat.format("PTC with component reference {0} cannot be started because it is currently being killed.", component_reference));
			return;
		case TC_EXITING:
		case TC_EXITED:
			send_error(tc.socket, MessageFormat.format("PTC with component reference {0} cannot be started because it is not alive anymore.", component_reference));
			return;
		case PTC_STALE:
			send_error(tc.socket, MessageFormat.format("The argument of starte operation ({0}) is a component reference that belongs to an earlier state.", component_reference));
			return;
		default:
			send_error(tc.socket, MessageFormat.format("Start operation was requested on component reference {0}, which is in invalid state.", component_reference));
			return;
		}
		target.tc_fn_name = new QualifiedName(module_name, function_name);
		target.stop_requested = false;
		boolean send_cancel_done = false;
		boolean cancel_any_component_done = false;

		/*
		TitanInteger argTitan = new TitanInteger();
		int arg = -1;
		if (text_buf.safe_pull_int(argTitan)) {
			//text_buf.pull_int().get_int();
			arg = argTitan.get_int();
		}

		// int arg = -1;
		byte return_value[] = new byte[text_buf.get_len() - text_buf.get_pos()];
		text_buf.pull_raw(text_buf.get_len() - text_buf.get_pos(), return_value);
		text_buf.cut_message();
		 */
		final byte[] arg = new byte[text_buf.get_len() - text_buf.get_pos()];
		text_buf.pull_raw(text_buf.get_len() - text_buf.get_pos(), arg);

		if (target.tc_state == tc_state_enum.PTC_STOPPED) {
			// updating the state of target because 'any component.done' cannot
			// consider this component anymore
			target.tc_state = tc_state_enum.PTC_STARTING;
			target.return_type = null;
			target.return_value = null;
			// determining which components we need to send CANCEL_DONE to
			target.cancel_done_sent_to = init_requestors(null);
			for (int i = 0;; i++) {
				final ComponentStruct comp = get_requestor(target.done_requestors, i);
				if (comp == null) {
					break;
				} else if (comp.equals(tc)) {
					// the start requestor shall cancel the done status locally ignore it
					continue;
				}
				switch (comp.tc_state) {
				case TC_CREATE:
				case TC_START:
				case TC_STOP:
				case TC_KILL:
				case TC_CONNECT:
				case TC_DISCONNECT:
				case TC_MAP:
				case TC_UNMAP:
				case TC_STOPPING:
				case MTC_TESTCASE:
				case PTC_FUNCTION:
				case PTC_STARTING:
				case PTC_STOPPED:
					// a CANCEL_DONE message shall be sent to comp
					send_cancel_done = true;
					add_requestor(target.cancel_done_sent_to, comp);
					break;
				case TC_EXITING:
				case TC_EXITED:
				case PTC_KILLING:
				case PTC_STOPPING_KILLING:
					// CANCEL_DONE will not be sent to comp
					break;
				default:
					error(MessageFormat.format("Test Component {0} is in invalid state when starting PTC {1}.", 
							comp.comp_ref, component_reference));
				}
			}
			// check whether 'any component.done' needs to be cancelled
			if (any_component_done_sent && !is_any_component_done()) {
				send_cancel_done = true;
				cancel_any_component_done = true;
				any_component_done_sent = false;
				add_requestor(target.cancel_done_sent_to, mtc);
			}
			free_requestors(target.done_requestors);
		}
		if (send_cancel_done) {
			for (int i = 0;; i++) {
				final ComponentStruct comp = get_requestor(target.cancel_done_sent_to, i);
				if (comp == null) {
					break;
				} else if (comp.equals(mtc)) {
					send_cancel_done_mtc(component_reference, cancel_any_component_done);
				} else {
					send_cancel_done_ptc(comp, component_reference);
				}
				add_requestor(comp.cancel_done_sent_for, target);
			}
			target.start_requestor = tc;
			tc.tc_state = tc_state_enum.TC_START;
			target.arg = arg;
		} else {
			send_start(target, target.tc_fn_name, arg);
			send_start_ack(tc);
			target.tc_state = tc_state_enum.PTC_FUNCTION;
		}

		status_change();
	}

	private void send_start_ack(final ComponentStruct tc) {
		final Text_Buf text_buf = new Text_Buf();
		text_buf.push_int(MSG_START_ACK);

		send_message(tc.socket, text_buf);
	}

	//private static void send_start(ComponentStruct target, QualifiedName tc_fn_name, int arg) {
	private void send_start(final ComponentStruct target, final QualifiedName tc_fn_name, final byte[] arg) {
		final Text_Buf text_buf = new Text_Buf();
		text_buf.push_int(MSG_START);
		text_buf.push_string(tc_fn_name.module_name);
		text_buf.push_string(tc_fn_name.definition_name);
		// text_buf.push_int(arg);
		text_buf.push_raw(arg);

		send_message(target.socket, text_buf);
	}

	private void send_cancel_done_ptc(final ComponentStruct comp, final int component_reference) {
		final Text_Buf text_buf = new Text_Buf();
		text_buf.push_int(MSG_CANCEL_DONE);
		text_buf.push_int(component_reference);

		send_message(comp.socket, text_buf);
	}

	private void send_cancel_done_mtc(final int component_reference, final boolean cancel_any_component_done) {
		final Text_Buf text_buf = new Text_Buf();
		text_buf.push_int(MSG_CANCEL_DONE);
		text_buf.push_int(component_reference);
		text_buf.push_int(cancel_any_component_done ? 1 : 0);

		send_message(mtc.socket, text_buf);
	}

	private boolean is_any_component_done() {
		for (int i = tc_first_comp_ref; i < components.size(); i++) {
			final ComponentStruct comp = components.get(i);
			if (component_is_done(comp)) {
				return true;
			}
		}

		return false;
	}

	private boolean component_is_done(final ComponentStruct tc) {
		switch (tc.tc_state) {
		case TC_EXITING:
		case TC_EXITED:
		case PTC_STOPPED:
			return true;
		case TC_INITIAL:
		case TC_IDLE:
		case TC_CREATE:
		case TC_START:
		case TC_STOP:
		case TC_KILL:
		case TC_CONNECT:
		case TC_DISCONNECT:
		case TC_MAP:
		case TC_UNMAP:
		case TC_STOPPING:
		case PTC_FUNCTION:
		case PTC_STARTING:
		case PTC_KILLING:
		case PTC_STOPPING_KILLING:
			return false;
		default:
			error(MessageFormat.format("PTC {0} is in invalid state when checking whether it is done.",
					tc.comp_ref));
			return false;
		}
	}

	private void process_connected(final ComponentStruct tc) {
		if (!message_expected(tc, "CONNECTED")) {
			return;
		}

		final Text_Buf text_buf = tc.text_buf;
		final String local_port = text_buf.pull_string();
		final int remote_component = text_buf.pull_int().get_int();
		final String remote_port = text_buf.pull_string();

		final PortConnection conn = find_connection(tc.comp_ref, local_port, remote_component, remote_port);
		if (conn != null) {
			// this message must arrive in the right state and from the server side (head)
			if (conn.conn_state == conn_state_enum.CONN_CONNECTING && conn.headComp == tc.comp_ref && conn.headPort.equals(local_port)) {
				send_connect_ack_to_requestors(conn);
				conn.conn_state = conn_state_enum.CONN_CONNECTED;
				status_change();
			} else {
				send_error(tc.socket, MessageFormat.format("Unexpected CONNECTED message was received "
						+ "for port connection {0}:{1} - {2}:{3}.", tc.comp_ref, local_port, remote_component, remote_port));
			}
		}
	}

	private void send_connect_ack_to_requestors(final PortConnection conn) {
		for (int i = 0;; i++) {
			final ComponentStruct comp = get_requestor(conn.requestors, i);
			if (comp == null) {
				break;
			} else if (comp.tc_state == tc_state_enum.TC_CONNECT) {
				send_connect_ack(comp);
				if (comp.equals(mtc)) {
					comp.tc_state = tc_state_enum.MTC_TESTCASE;
				} else {
					comp.tc_state = tc_state_enum.PTC_FUNCTION;
				}
			}
		}

		free_requestors(conn.requestors);
	}

	private void process_connect_listen_ack(final ComponentStruct tc, final int msg_end) {
		if (!message_expected(tc, "CONNECT_LISTEN_ACK")) {
			return;
		}

		final Text_Buf text_buf = tc.text_buf;
		final String local_port = text_buf.pull_string();
		final int remote_component = text_buf.pull_int().get_int();
		final String remote_port = text_buf.pull_string();
		final int transport_type = text_buf.pull_int().get_int();

		// FIXME check addr
		final byte temp[] = new byte[2];
		final byte local_port_number[] = new byte[2];
		text_buf.pull_raw(2, temp);
		text_buf.pull_raw(2, local_port_number);
		final int local_addr_begin = text_buf.get_pos();
		final int local_addr_len = msg_end - local_addr_begin;

		byte addr[];
		if (local_addr_len == 12) {
			addr = new byte[4];
			text_buf.pull_raw(4, addr);
			final byte zero[] = new byte[8];
			text_buf.pull_raw(8, zero);
		} else {
			addr = new byte[16];
			text_buf.pull_raw(16, addr);
		}
		InetAddress address = null;
		try {
			address = InetAddress.getByAddress(addr);
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}

		final PortConnection conn = find_connection(tc.comp_ref, local_port, remote_component, remote_port);
		if (conn != null) {
			// this message must arrive in the right state
			// and from the server side (head)
			if (conn.conn_state != conn_state_enum.CONN_LISTENING || conn.headComp != tc.comp_ref
					|| conn.headPort.compareTo(local_port) != 0) {
				send_error(tc.socket, MessageFormat.format("Unexpected message CONNECT_LISTEN_ACK was received "
						+ "for port connection {0}:{1} - {2}:{3}.", tc.comp_ref, local_port, remote_component, remote_port));
				return;
			} else if (conn.transport_type.ordinal() != transport_type) {
				send_error(tc.socket, MessageFormat.format("Message CONNECT_LISTEN_ACK for port connection {0}:{1} - {2}:{3} contains wrong transport type: {4} was expected instead of {5}.",
						tc.comp_ref, local_port, remote_component, remote_port, conn.transport_type.toString(), transport_type_enum.values()[transport_type].toString()));
				return;
			}
			final ComponentStruct dst_comp = components.get(remote_component);
			switch (dst_comp.tc_state) {
			case TC_IDLE:
			case TC_CREATE:
			case TC_START:
			case TC_STOP:
			case TC_KILL:
			case TC_CONNECT:
			case TC_DISCONNECT:
			case TC_MAP:
			case TC_UNMAP:
			case TC_STOPPING:
			case MTC_TESTCASE:
			case PTC_FUNCTION:
			case PTC_STARTING:
			case PTC_STOPPED:
				if (tc.comp_ref != TitanComponent.MTC_COMPREF && tc.comp_ref != remote_component) {
					// send the name of tc
					send_connect(dst_comp, remote_port, tc.comp_ref, tc.comp_name, local_port,
							transport_type_enum.values()[transport_type], address, local_port_number);
				} else {
					// send an empty string instead of the name of tc if it is known by dst_comp
					send_connect(dst_comp, remote_port, tc.comp_ref, null, local_port,
							transport_type_enum.values()[transport_type], address, local_port_number);
				}
				conn.conn_state = conn_state_enum.CONN_CONNECTING;
				break;
			default:
				send_disconnect_to_server(conn);
				send_error_to_connect_requestors(conn, "test component " + dst_comp + " has terminated during connection setup.");
				remove_connection(conn);
				break;
			}
			status_change();
		} else {
			// the connection does not exist anymore
			// check whether the transport type is valid
			switch (transport_type_enum.values()[transport_type]) {
			case TRANSPORT_LOCAL:
				send_error(tc.socket, MessageFormat.format("Message CONNECT_LISTEN_ACK for port connection {0}:{1} - {2}:{3} cannot refer to transport type {4}.",
						tc.comp_ref, local_port, remote_component, remote_port, transport_type_enum.values()[transport_type].toString()));

				break;
			case TRANSPORT_INET_STREAM:
				break;
			default:
				send_error(tc.socket, MessageFormat.format("Message CONNECT_LISTEN_ACK for port connection "
						+ "{0}:{1} - {2}:{3} refers to invalid transport type {4}.", tc.comp_ref, local_port,
						remote_component, remote_port, transport_type_enum.values()[transport_type].toString()));
				break;
			}
		}
	}

	private void send_error_to_connect_requestors(final PortConnection conn, final String msg) {
		final String reason = "Establishment of port connection " + conn.headComp + ":" + conn.headPort + " - " + conn.tailComp + ":"
				+ conn.tailPort + " failed because " + msg;
		for (int i = 0;; i++) {
			final ComponentStruct comp = get_requestor(conn.requestors, i);
			if (comp == null) {
				break;
			} else if (comp.tc_state == tc_state_enum.TC_CONNECT) {
				send_error(comp.socket, reason);
				if (comp.equals(mtc)) {
					comp.tc_state = tc_state_enum.MTC_TESTCASE;
				} else {
					comp.tc_state = tc_state_enum.PTC_FUNCTION;
				}
			}
		}
	}

	private static ComponentStruct get_requestor(final RequestorStruct reqs, final int index) {
		if (index >= 0 && index < reqs.components.size()) {
			return reqs.components.get(index);
		}

		return null;
	}

	private static void free_requestors(final RequestorStruct requestor) {
		requestor.components.clear();
	}

	private void process_connect_error(final ComponentStruct tc) {
		if (!message_expected(tc, "CONNECT_ERROR")) {
			return;
		}

		final Text_Buf text_buf = tc.text_buf;
		final String localPort = text_buf.pull_string();
		final int remote_comp = text_buf.pull_int().get_int();
		final String remote_port = text_buf.pull_string();
		final String message = text_buf.pull_string();

		final PortConnection conn = find_connection(tc.comp_ref, localPort, remote_comp, remote_port);
		if (conn != null) {
			switch (conn.conn_state) {
			case CONN_CONNECTING:
				// in this state both endpoints can report error
				if (conn.transport_type != transport_type_enum.TRANSPORT_LOCAL && conn.tailComp == tc.comp_ref
						&& conn.tailPort.equals(localPort)) {
					// shut down the server side (head) only if the error was reported by the client side (tail)
					send_disconnect_to_server(conn);
				}
				break;
			case CONN_LISTENING:
				// in this state only the server side (head) can report the error
				if (conn.headComp == tc.comp_ref && conn.headPort.equals(localPort)) {
					break;
				}
			default:
				send_error(tc.socket, MessageFormat.format("Unexpected message CONNECT_ERROR was received for "
						+ "port connection {0}:{1} - {2}:{3}.", tc.comp_ref, localPort, remote_comp, remote_port));
				return;
			}
			send_error_to_connect_requestors(conn, "test component " + tc.comp_ref + " reported error: " + message);
			remove_connection(conn);
			status_change();
		}
	}

	private void remove_connection(final PortConnection c) {
		// Remove from conn_head_list
		final ComponentStruct head_component = lookup_component(c.headComp);
		head_component.conn_head_list.remove(c);

		// Remove from conn_tail_list
		final ComponentStruct tail_component = lookup_component(c.tailComp);
		tail_component.conn_tail_list.remove(c);

		free_requestors(c.requestors);
	}

	private void send_disconnect_to_server(final PortConnection conn) {
		final ComponentStruct comp = components.get(conn.headComp);
		switch (comp.tc_state) {
		case TC_IDLE:
		case TC_CREATE:
		case TC_START:
		case TC_STOP:
		case TC_KILL:
		case TC_CONNECT:
		case TC_DISCONNECT:
		case TC_MAP:
		case TC_UNMAP:
		case TC_STOPPING:
		case MTC_TESTCASE:
		case PTC_FUNCTION:
		case PTC_STARTING:
		case PTC_STOPPED:
			send_disconnect(comp, conn.headPort, conn.tailComp, conn.tailPort);
		default:
			break;
		}

	}

	private void process_connect_req(final ComponentStruct tc) {
		if (!request_allowed(tc, "CONNECT_REQ")) {
			return;
		}

		final Text_Buf text_buf = tc.text_buf;
		final int sourceComponent = text_buf.pull_int().get_int();
		final String sourcePort = text_buf.pull_string();
		final int destinationComponent = text_buf.pull_int().get_int();
		final String destinationPort = text_buf.pull_string();

		if (!valid_endpoint(sourceComponent, true, tc, "connect") || !valid_endpoint(destinationComponent, true, tc, "connect")) {
			return;
		}

		PortConnection conn = find_connection(sourceComponent, sourcePort, destinationComponent, destinationPort);
		if (conn == null) {
			conn = new PortConnection();
			conn.transport_type = choose_port_connection_transport(sourceComponent, destinationComponent);
			conn.headComp = sourceComponent;
			conn.headPort = sourcePort;
			conn.tailComp = destinationComponent;
			conn.tailPort = destinationPort;
			conn.requestors = init_requestors(tc);
			add_connection(conn);

			// conn.head and tail is now in canonical order
			switch (conn.transport_type) {
			case TRANSPORT_LOCAL:
				// send an empty string instead of component name
				// the component should already know its own name
				send_connect(components.get(conn.headComp), conn.headPort, conn.tailComp, null, conn.tailPort, conn.transport_type, null, null);
				conn.conn_state = conn_state_enum.CONN_CONNECTING;
				break;
			case TRANSPORT_INET_STREAM:
				// conn->head will be the server side
				if (conn.tailComp != TitanComponent.MTC_COMPREF && conn.tailComp != conn.headComp) {
					// send the name of conn->tail
					send_connect_listen(components.get(conn.headComp), conn.headPort, conn.tailComp,
							components.get(conn.tailComp).comp_name, conn.tailPort, conn.transport_type);
				} else {
					// send an empty string instead of the name of conn->tail if
					// it is known by conn->head
					send_connect_listen(components.get(conn.headComp), conn.headPort, conn.tailComp, null,
							conn.tailPort, conn.transport_type);
				}
				conn.conn_state = conn_state_enum.CONN_LISTENING;
				break;
			default:
				send_error(tc.socket, MessageFormat.format("The port connection {0}:{1} - {2}:{3} cannot be established because no suitable transport mechanism is available on the corrensponding host(s).",
						sourceComponent, sourcePort, destinationComponent, destinationPort));
				remove_connection(conn);
				return;
			}
			tc.tc_state = tc_state_enum.TC_CONNECT;
			status_change();
		} else {
			switch (conn.conn_state) {
			case CONN_LISTENING:
			case CONN_CONNECTING:
				add_requestor(conn.requestors, tc);
				tc.tc_state = tc_state_enum.TC_CONNECT;
				status_change();
				break;
			case CONN_CONNECTED:
				send_connect_ack(tc);
				break;
			case CONN_DISCONNECTING:
				send_error(tc.socket, MessageFormat.format("The port connection {0}:{1} - {2}:{3} cannot be established because a disconnect operation is in progress on it.",
						sourceComponent, sourcePort, destinationComponent, destinationPort));
				break;
			default:
				send_error(tc.socket, MessageFormat.format("The port connection {0}:{1} - {2}:{3} cannot be established due to an internal error in the MC.",
						sourceComponent, sourcePort, destinationComponent, destinationPort));
				error(MessageFormat.format("The port connection {0}:{1} - {2}:{3} is in invalid state when a connect operation was requested on it.",
						sourceComponent, sourcePort, destinationComponent, destinationPort));
			}
		}
	}

	private void add_requestor(final RequestorStruct reqs, final ComponentStruct tc) {
		if (reqs.components.contains(tc)) {
			return;
		}

		reqs.components.add(tc);
	}

	private void send_connect_listen(final ComponentStruct tc, final String headPort, final int tailComp, final String comp_name,
			final String tailPort, final transport_type_enum transport_type) {
		final Text_Buf text_buf = new Text_Buf();
		text_buf.push_int(MSG_CONNECT_LISTEN);
		text_buf.push_string(headPort);
		text_buf.push_int(tailComp);
		text_buf.push_string(comp_name);
		text_buf.push_string(tailPort);
		text_buf.push_int(transport_type.ordinal());

		send_message(tc.socket, text_buf);
	}

	private void send_connect(final ComponentStruct tc, final String headPort, final int tailComp, final String compName,
			final String tailPort, final transport_type_enum transport_type, final InetAddress address, final byte[] local_port_number) {
		final Text_Buf text_buf = new Text_Buf();
		text_buf.push_int(MSG_CONNECT);
		text_buf.push_string(headPort);
		text_buf.push_int(tailComp);
		text_buf.push_string(compName);
		text_buf.push_string(tailPort);
		text_buf.push_int(transport_type.ordinal());
		if (address != null) {
			if (address instanceof Inet4Address) {
				final byte temp[] = address.getAddress();
				text_buf.push_raw(2, new byte[] { 2, 0 });
				text_buf.push_raw(local_port_number.length, local_port_number);
				text_buf.push_raw(temp.length, temp);
				text_buf.push_raw(8, new byte[8]);
			} else if (address instanceof Inet6Address) {
				final Inet6Address localipv6_address = (Inet6Address) address;
				final byte temp[] = localipv6_address.getAddress();
				text_buf.push_raw(2, new byte[] { 2, 3 });
				text_buf.push_raw(local_port_number.length, local_port_number);
				text_buf.push_raw(temp.length, temp);
				text_buf.push_int(localipv6_address.getScopeId());
			}
		}

		send_message(tc.socket, text_buf);
	}

	private void send_connect_ack(final ComponentStruct tc) {
		final Text_Buf text_buf = new Text_Buf();
		text_buf.push_int(MSG_CONNECT_ACK);
		send_message(tc.socket, text_buf);
	}

	private transport_type_enum choose_port_connection_transport(final int sourceComponent, final int destinationComponent) {
		final Host headLoc = components.get(sourceComponent).comp_location;
		if (sourceComponent == destinationComponent && headLoc.transport_supported[transport_type_enum.TRANSPORT_LOCAL.ordinal()]) {
			return transport_type_enum.TRANSPORT_LOCAL;
		}

		final Host tailLoc = components.get(destinationComponent).comp_location;
		if (headLoc.transport_supported[transport_type_enum.TRANSPORT_INET_STREAM.ordinal()]
				&& tailLoc.transport_supported[transport_type_enum.TRANSPORT_INET_STREAM.ordinal()]) {
			return transport_type_enum.TRANSPORT_INET_STREAM;
		}

		return transport_type_enum.TRANSPORT_NUM;
	}

	private PortConnection find_connection(int sourceComponent, String sourcePort, int destinationComponent, String destinationPort) {

		if (sourceComponent > destinationComponent) {
			final int tmp_comp = sourceComponent;
			sourceComponent = destinationComponent;
			destinationComponent = tmp_comp;
			final String tmp_port = sourcePort;
			sourcePort = destinationPort;
			destinationPort = tmp_port;
		} else if (sourceComponent == destinationComponent && sourcePort.compareTo(destinationPort) > 0) {
			final String tmp_port = sourcePort;
			sourcePort = destinationPort;
			destinationPort = tmp_port;
		}

		final ComponentStruct headComp = lookup_component(sourceComponent);

		final List<PortConnection> headConn = headComp.conn_head_list;
		if (headConn == null) {
			return null;
		}

		final ComponentStruct tailComp = lookup_component(destinationComponent);
		final List<PortConnection> tailConn = tailComp.conn_tail_list;
		if (tailConn == null) {
			return null;
		}

		if (headComp.conn_head_list.size() <= tailComp.conn_tail_list.size()) {
			for (final PortConnection pc : headConn) {
				if (pc.tailComp == destinationComponent && pc.headPort.equals(sourcePort) && pc.tailPort.equals(destinationPort)) {
					return pc;
				}
			}
			return null;
		} else {
			for (final PortConnection pc : tailConn) {
				if (pc.headComp == sourceComponent && pc.headPort.equals(sourcePort) && pc.tailPort.equals(destinationPort)) {
					return pc;
				}
			}
			return null;
		}
	}

	private void remove_all_connections(final int head_or_tail) {
		final ComponentStruct component = lookup_component(head_or_tail);
		if (component == null) {
			return;
		}

		while (component.conn_head_list != null && !component.conn_head_list.isEmpty()) {
			remove_connection(component.conn_head_list.get(0));
		}
		while (component.conn_tail_list != null && !component.conn_tail_list.isEmpty()) {
			remove_connection(component.conn_tail_list.get(0));
		}
	}

	private void add_connection(final PortConnection pc) {
		if (pc.headComp > pc.tailComp) {
			final int tmp_comp = pc.headComp;
			pc.headComp = pc.tailComp;
			pc.tailComp = tmp_comp;
			final String tmp_port = pc.headPort;
			pc.headPort = pc.tailPort;
			pc.tailPort = tmp_port;
		} else if (pc.headComp == pc.tailComp && pc.headPort.compareTo(pc.tailPort) > 0) {
			final String tmp_port = pc.headPort;
			pc.headPort = pc.tailPort;
			pc.tailPort = tmp_port;
		}

		final ComponentStruct headComp = lookup_component(pc.headComp);
		if (headComp.conn_head_list == null) {
			headComp.conn_head_list = new ArrayList<PortConnection>();
		}
		headComp.conn_head_list.add(pc);

		final ComponentStruct tailComp = lookup_component(pc.tailComp);
		if (tailComp.conn_tail_list == null) {
			tailComp.conn_tail_list = new ArrayList<PortConnection>();
		}
		tailComp.conn_tail_list.add(pc);
	}

	private boolean valid_endpoint(final int component_reference, final boolean new_connection, final ComponentStruct requestor,
			final String operation) {
		switch (component_reference) {
		case TitanComponent.NULL_COMPREF:
			send_error(requestor.socket, MessageFormat.format("The {0} refers to the null component reference.", operation));
			return false;
		case TitanComponent.SYSTEM_COMPREF:
			send_error(requestor.socket, MessageFormat.format("The {0} refers to the system component reference.", operation));
			return false;
		case TitanComponent.ANY_COMPREF:
			send_error(requestor.socket, MessageFormat.format("The {0} refers to 'any component'.", operation));
			return false;
		case TitanComponent.ALL_COMPREF:
			send_error(requestor.socket, "The " + operation + " refers to 'all component'.");
			send_error(requestor.socket, MessageFormat.format("The {0} refers to 'all component'.", operation));
			return false;
		default:
			break;
		}

		final ComponentStruct comp = lookup_component(component_reference);
		if (comp == null) {
			send_error(requestor.socket, MessageFormat.format("The {0} refers to invalid component " + "reference {1}.",
					operation, component_reference));
			return false;
		}

		switch (comp.tc_state) {
		case TC_IDLE:
		case TC_CREATE:
		case TC_START:
		case TC_STOP:
		case TC_KILL:
		case TC_CONNECT:
		case TC_DISCONNECT:
		case TC_MAP:
		case TC_UNMAP:
		case TC_STOPPING:
		case MTC_TESTCASE:
		case PTC_FUNCTION:
		case PTC_STARTING:
		case PTC_STOPPED:
			return true;
		case PTC_KILLING:
		case PTC_STOPPING_KILLING:
			if (new_connection) {
				send_error(requestor.socket, MessageFormat.format("The {0} refers to test component with "
						+ "component reference {1}, which is currently being terminated.", operation, component_reference));
				return false;
			} else {
				return true;
			}
		case TC_EXITING:
		case TC_EXITED:
			if (new_connection) {
				send_error(requestor.socket, MessageFormat.format("The {0} refers to test component with "
						+ "component reference {1}, which has already terminated.", operation, component_reference));
				return false;
			} else {
				return true;
			}
		case PTC_STALE:
			send_error(requestor.socket, MessageFormat.format("The {0} refers to component reference {1} "
					+ ", which belongs to an earlier test case.", operation, component_reference));
			return false;
		default:
			send_error(requestor.socket, MessageFormat.format("The {0} refers to component reference {1} "
					+ ", which is in invalid state.", operation, component_reference));
			return false;
		}

	}

	private void process_mtc_ready() {
		if (mc_state != mcStateEnum.MC_EXECUTING_CONTROL || mtc.tc_state != tc_state_enum.MTC_CONTROLPART) {
			send_error(mtc.socket, "Unexpected message MTC_READY was received.");
			return;
		}

		mc_state = mcStateEnum.MC_READY;
		mtc.tc_state = tc_state_enum.TC_IDLE;
		mtc.stop_requested = false;
		if (mtc.kill_timer != null) {
			cancel_timer(mtc.kill_timer);
			mtc.kill_timer = null;
		}

		stop_requested = false;
		notify("Test execution finished.");
		status_change();
	}

	public void shutdown_session() {
		lock();
		switch (mc_state) {
		case MC_INACTIVE:
			status_change();
			break;
		case MC_SHUTDOWN:
			break;
		case MC_LISTENING:
		case MC_LISTENING_CONFIGURED:
		case MC_HC_CONNECTED:
		case MC_ACTIVE:
			notify("Shutting down session.");
			wakeup_thread(wakeup_reason_t.REASON_SHUTDOWN);
			break;
		default:
			error("MainController::shutdown_session: called in wrong state.");
		}
		unlock();

	}

	private void process_ptc_created(final unknown_connection connection) {
		switch (mc_state) {
		case MC_EXECUTING_TESTCASE:
		case MC_TERMINATING_TESTCASE:
			break;
		default:
			send_error(connection.channel, "Message PTC_CREATED arrived in invalid state.");
			close_unknown_connection(connection);
			return;
		}

		final Text_Buf text_buf = connection.text_buf;
		final int component_reference = text_buf.pull_int().get_int();

		switch (component_reference) {
		case TitanComponent.NULL_COMPREF:
			send_error(connection.channel, "Message PTC_CREATED refers to the null component reference.");
			close_unknown_connection(connection);
			return;
		case TitanComponent.MTC_COMPREF:
			send_error(connection.channel, "Message PTC_CREATED refers to the component reference of the MTC.");
			close_unknown_connection(connection);
			return;
		case TitanComponent.SYSTEM_COMPREF:
			send_error(connection.channel, "Message PTC_CREATED refers to the component reference of the system.");
			close_unknown_connection(connection);
			return;
		case TitanComponent.ANY_COMPREF:
			send_error(connection.channel, "Message PTC_CREATED refers to 'any component'.");
			close_unknown_connection(connection);
			return;
		case TitanComponent.ALL_COMPREF:
			send_error(connection.channel, "Message PTC_CREATED refers to 'all component'.");
			close_unknown_connection(connection);
			return;
		}

		final ComponentStruct tc = lookup_component(component_reference);
		if (tc == null) {
			send_error(connection.channel, MessageFormat.format("Message PTC_CREATED referes to invalid component reference {0}.", component_reference));
			close_unknown_connection(connection);
			return;
		} else if (tc.tc_state != tc_state_enum.TC_INITIAL) {
			send_error(connection.channel, MessageFormat.format("Message PTC_CREATED refers to test component {0}, which is not "
					+ "being created.", component_reference));
			close_unknown_connection(connection);
			return;
		} // FIXME extra branch conn_ip_addr

		tc.tc_state = tc_state_enum.TC_IDLE;
		tc.socket = connection.channel;

		final channel_table_struct new_struct = new channel_table_struct();
		new_struct.channel_type = channel_type_enum.CHANNEL_TC;
		new_struct.component = tc;
		channel_table.put(connection.channel, new_struct);
		text_buf.cut_message();
		tc.text_buf = text_buf;

		delete_unknown_connection(connection);

		if (mc_state == mcStateEnum.MC_TERMINATING_TESTCASE || mtc.stop_requested || mtc.tc_state == tc_state_enum.MTC_ALL_COMPONENT_KILL
				|| (mtc.tc_state == tc_state_enum.MTC_ALL_COMPONENT_STOP && !tc.is_alive)) {
			send_kill(tc);
			tc.tc_state = tc_state_enum.PTC_KILLING;
			if (!tc.is_alive) {
				tc.stop_requested = true;
			}

			tc.stop_requestors = init_requestors(null);
			tc.kill_requestors = init_requestors(null);
			start_kill_timer(tc);
		} else {
			if (tc.create_requestor.tc_state == tc_state_enum.TC_CREATE) {
				send_create_ack(tc.create_requestor, component_reference);
				if (tc.create_requestor.equals(mtc)) {
					tc.create_requestor.tc_state = tc_state_enum.MTC_TESTCASE;
				} else {
					tc.create_requestor.tc_state = tc_state_enum.PTC_FUNCTION;
				}
			}
		}

		handle_tc_data(tc, false);
		status_change();
	}

	private void send_kill(final ComponentStruct tc) {
		final Text_Buf text_buf = new Text_Buf();
		text_buf.push_int(MSG_KILL);
		send_message(tc.socket, text_buf);
	}

	private void send_kill_process(final Host hc, final int component_reference) {
		final Text_Buf text_buf = new Text_Buf();
		text_buf.push_int(MSG_KILL_PROCESS);
		text_buf.push_int(component_reference);
		send_message(hc.socket, text_buf);
	}

	private void send_create_ack(final ComponentStruct create_requestor, final int component_reference) {
		final Text_Buf text_buf = new Text_Buf();
		text_buf.push_int(MSG_CREATE_ACK);
		text_buf.push_int(component_reference);
		send_message(create_requestor.socket, text_buf);
	}

	public boolean request_allowed(final ComponentStruct from, final String message_name) {
		if (!message_expected(from, message_name)) {
			return false;
		}

		switch (from.tc_state) {
		case MTC_TESTCASE:
			if (from.equals(mtc)) {
				return true;
			} else {
				break;
			}
		case PTC_FUNCTION:
			if (!from.equals(mtc)) {
				return true;
			} else {
				break;
			}
		case TC_STOPPING:
		case PTC_STOPPING_KILLING:
		case PTC_KILLING:
			// silently ignore
			return false;
		default:
			break;
		}
		notify(MessageFormat.format("The sender of message {0} is in unexpected state: {1}.", message_name, from.tc_state.toString()));
		send_error(from.socket, MessageFormat.format("The sender of message {0} is in unexpected state.", message_name));
		return false;
	}

	private boolean message_expected(final ComponentStruct from, final String message_name) {
		switch (mc_state) {
		case MC_EXECUTING_TESTCASE:
			switch (mtc.tc_state) {
			case MTC_ALL_COMPONENT_STOP:
			case MTC_ALL_COMPONENT_KILL:
				// silently ignore
				return false;
			default:
				return true;
			}
		case MC_TERMINATING_TESTCASE:
			// silently ignore
			return false;
		default:
			send_error(from.socket, MessageFormat.format("Unexpected message {0} was received.", message_name));
			return false;
		}
	}

	private void close_connection(final Host hc) {
		try {
			hc.socket.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		hosts.remove(hc);
	}

	private void process_testcase_finished() {
		if (mc_state != mcStateEnum.MC_EXECUTING_TESTCASE) {
			send_error(mtc.socket, "Unexpected message TESTCASE_FINISHED was received.");
			return;
		}

		final boolean ready_to_finish = kill_all_components(true);

		final Text_Buf local_incoming_buf = mtc.text_buf;
		mc_state = mcStateEnum.MC_TERMINATING_TESTCASE;
		mtc.tc_state = tc_state_enum.MTC_TERMINATING_TESTCASE;
		final int verdict = local_incoming_buf.pull_int().get_int();
		final String reason = local_incoming_buf.pull_string();
		mtc.local_verdict = VerdictTypeEnum.values()[verdict];
		mtc.verdict_reason = reason;
		mtc.stop_requested = false;
		if (mtc.kill_timer != null) {
			cancel_timer(mtc.kill_timer);
			mtc.kill_timer = null;
		}

		any_component_done_requested = false;
		any_component_done_sent = false;
		all_component_done_requested = false;
		any_component_killed_requested = false;
		all_component_killed_requested = false;

		if (ready_to_finish) {
			finish_testcase();
		}

		status_change();
	}

	private void finish_testcase() {
		if (stop_requested) {
			send_ptc_verdict(false);
			send_stop(mtc);
			mtc.tc_state = tc_state_enum.MTC_CONTROLPART;
			mtc.stop_requested = true;
			start_kill_timer(mtc);
			mc_state = mcStateEnum.MC_EXECUTING_CONTROL;
		} else if (stop_after_tc) {
			send_ptc_verdict(false);
			mtc.tc_state = tc_state_enum.MTC_PAUSED;
			mc_state = mcStateEnum.MC_PAUSED;
			notify("Execution has been paused.");
		} else {
			send_ptc_verdict(true);
			mtc.tc_state = tc_state_enum.MTC_CONTROLPART;
			mc_state = mcStateEnum.MC_EXECUTING_CONTROL;
		}

		for (int i = tc_first_comp_ref; i < components.size(); i++) {
			final ComponentStruct comp = components.get(i);
			comp.tc_state = tc_state_enum.PTC_STALE;
		}

		mtc.local_verdict = VerdictTypeEnum.NONE;
		mtc.comp_type = new QualifiedName("", "");
		mtc.tc_fn_name = new QualifiedName("", "");
		system.comp_type = new QualifiedName("", "");
	}

	private void send_ptc_verdict(final boolean continue_execution) {
		final Text_Buf text_buf = new Text_Buf();
		text_buf.push_int(MSG_PTC_VERDICT);
		int n_ptcs = 0;
		for (int i = tc_first_comp_ref; i < components.size(); i++) {
			final ComponentStruct comp = components.get(i);
			if (comp.tc_state != tc_state_enum.PTC_STALE) {
				n_ptcs++;
			}
		}

		text_buf.push_int(n_ptcs);
		for (int i = tc_first_comp_ref; i < components.size(); i++) {
			final ComponentStruct comp = components.get(i);
			if (comp.tc_state != tc_state_enum.PTC_STALE) {
				text_buf.push_int(comp.comp_ref);
				text_buf.push_string(comp.comp_name);
				text_buf.push_int(comp.local_verdict.getValue());
				// FIXME: check verdict_reason possible null?
				text_buf.push_string(comp.verdict_reason);
			}
		}

		text_buf.push_int(continue_execution ? 1 : 0);
		send_message(mtc.socket, text_buf);
	}

	private void process_testcase_started() {
		if (mc_state != mcStateEnum.MC_EXECUTING_CONTROL) {
			send_error(mtc.socket, "Unexpected message TESTCASE_STARTED was received.");
			return;
		}

		final Text_Buf text_buf = mtc.text_buf;
		mtc.tc_fn_name = new QualifiedName(text_buf.pull_string(), text_buf.pull_string());
		mtc.comp_type = new QualifiedName(text_buf.pull_string(), text_buf.pull_string());
		system.comp_type = new QualifiedName(text_buf.pull_string(), text_buf.pull_string());

		mtc.tc_state = tc_state_enum.MTC_TESTCASE;
		mc_state = mcStateEnum.MC_EXECUTING_TESTCASE;
		tc_first_comp_ref = next_comp_ref;

		any_component_done_requested = false;
		any_component_done_sent = false;
		all_component_done_requested = false;
		any_component_killed_requested = false;
		all_component_killed_requested = false;

		status_change();
	}

	private void process_log(final unknown_connection connection) {
		final Text_Buf text_buf = connection.text_buf;
		final int upper_int = text_buf.pull_int().get_int();
		final int lower_int = text_buf.pull_int().get_int();
		final long seconds = upper_int * 0xffffffff + lower_int;
		final int microseconds = text_buf.pull_int().get_int();
		final String source = MessageFormat.format("<unknown>@{0}", connection.ip_address.getHostAddress());
		final int severity = text_buf.pull_int().get_int();

		final int length = text_buf.pull_int().get_int();
		final byte messageBytes[] = new byte[length];
		text_buf.pull_raw(length, messageBytes);
		notify(new Timeval(seconds, microseconds), source, severity, new String(messageBytes));
	}

	private void process_log(final Host hc) {
		final Text_Buf text_buf = hc.text_buf;
		final int upper_int = text_buf.pull_int().get_int();
		final int lower_int = text_buf.pull_int().get_int();
		final long seconds = upper_int * 0xffffffff + lower_int;
		final int microseconds = text_buf.pull_int().get_int();
		final int severity = text_buf.pull_int().get_int();

		//FIXME: length can't be below zero
		final int length = text_buf.pull_int().get_int();
		final byte messageBytes[] = new byte[length];
		text_buf.pull_raw(length, messageBytes);
		notify(new Timeval(seconds, microseconds), hc.log_source, severity, new String(messageBytes));
	}

	private void process_log(final ComponentStruct tc) {
		final Text_Buf text_buf = tc.text_buf;
		final int upper_int = text_buf.pull_int().get_int();
		final int lower_int = text_buf.pull_int().get_int();
		final long seconds = upper_int * 0xffffffff + lower_int;
		final int microseconds = text_buf.pull_int().get_int();
		final int severity = text_buf.pull_int().get_int();

		final int length = text_buf.pull_int().get_int();
		final byte messageBytes[] = new byte[length];
		text_buf.pull_raw(length, messageBytes);
		notify(new Timeval(seconds, microseconds), tc.log_source, severity, new String(messageBytes));
	}

	public void execute_testcase(final String moduleName, final String testcaseName) {
		lock();
		if (mc_state != mcStateEnum.MC_READY) {
			error("MainController::execute_testcase: called in wrong state.");
			unlock();
			return;
		}

		send_execute_testcase(moduleName, testcaseName);
		mc_state = mcStateEnum.MC_EXECUTING_CONTROL;
		mtc.tc_state = tc_state_enum.MTC_CONTROLPART;

		status_change();
		unlock();
	}

	public void stop_after_testcase(final boolean newState) {
		lock();
		stop_after_tc = newState;
		if (mc_state == mcStateEnum.MC_PAUSED && !stop_after_tc) {
			unlock();
			continue_testcase();
		} else {
			unlock();
		}
	}

	public void continue_testcase() {
		lock();
		if (mc_state == mcStateEnum.MC_PAUSED) {
			notify("Resuming execution.");
			send_continue();
			mtc.tc_state = tc_state_enum.MTC_CONTROLPART;
			mc_state = mcStateEnum.MC_EXECUTING_CONTROL;
			ui.status_change();
		} else {
			error("MainController::continue_testcase: called in wrong state.");
		}
		unlock();
	}

	private void send_continue() {
		final Text_Buf text_buf = new Text_Buf();
		text_buf.push_int(MSG_CONTINUE);
		send_message(mtc.socket, text_buf);
	}

	public void stop_execution() {
		lock();
		if (!stop_requested) {
			notify("Stopping execution.");
			switch (mc_state) {
			case MC_PAUSED:
				mc_state = mcStateEnum.MC_EXECUTING_CONTROL;
				mtc.tc_state = tc_state_enum.MTC_CONTROLPART;
				// no break
			case MC_EXECUTING_CONTROL:
				send_stop(mtc);
				mtc.stop_requested = true;
				start_kill_timer(mtc);
				wakeup_thread(wakeup_reason_t.REASON_MTC_KILL_TIMER);
				break;
			case MC_EXECUTING_TESTCASE:
				if (!mtc.stop_requested) {
					send_stop(mtc);
					kill_all_components(true);
					mtc.stop_requested = true;
					start_kill_timer(mtc);
					wakeup_thread(wakeup_reason_t.REASON_MTC_KILL_TIMER);
				}
			case MC_TERMINATING_TESTCASE:
				// MTC will be stopped later in finish_testcase()
			case MC_READY:
				// do nothing
				break;
			default:
				error("MainController::stop_execution: called in wrong state.");
				unlock();
				return;
			}
			stop_requested = true;
			status_change();
		} else {
			notify("Stop was already requested. Operation ignored.");
		}

		unlock();
	}

	private void send_execute_testcase(final String moduleName, final String testcaseName) {
		final Text_Buf text_buf = new Text_Buf();
		text_buf.push_int(MSG_EXECUTE_TESTCASE);
		text_buf.push_string(moduleName);
		text_buf.push_string(testcaseName);
		send_message(mtc.socket, text_buf);
	}

	public void execute_control(final String module_name) {
		lock();
		if (mc_state != mcStateEnum.MC_READY) {
			error("MainController::execute_control: called in wrong state.");
			unlock();
			return;
		}

		send_execute_control(module_name);
		mc_state = mcStateEnum.MC_EXECUTING_CONTROL;
		mtc.tc_state = tc_state_enum.MTC_CONTROLPART;
		status_change();
		unlock();
	}

	private void send_execute_control(final String module_name) {
		final Text_Buf text_buf = new Text_Buf();
		text_buf.push_int(MSG_EXECUTE_CONTROL);
		text_buf.push_string(module_name);
		send_message(mtc.socket, text_buf);
	}

	private static RequestorStruct init_requestors(final ComponentStruct tc) {
		final RequestorStruct requestors = new RequestorStruct();
		if (tc != null) {
			requestors.components.add(tc);
		}

		return requestors;
	}

	public void exit_mtc() {
		lock();
		if (mc_state != mcStateEnum.MC_READY && mc_state != mcStateEnum.MC_RECONFIGURING) {
			error("MainController::exit_mtc: called in wrong state.");
			unlock();
			return;
		}
		notify("Terminating MTC.");
		send_exit_mtc();

		mtc.tc_state = tc_state_enum.TC_EXITING;
		mtc.comp_location.n_active_components--;
		mc_state = mcStateEnum.MC_TERMINATING_MTC;
		start_kill_timer(mtc);
		status_change();
		unlock();
	}

	private void send_exit_mtc() {
		final Text_Buf text_buf = new Text_Buf();
		text_buf.push_int(MSG_EXIT_MTC);
		send_message(mtc.socket, text_buf);
	}

	private void shutdown_server() {
		if (mc_channel != null) {
			try {
				mc_channel.close();
			} catch (IOException e) {
				// FIXME handle
			}
			channel_table.remove(mc_channel);
			mc_channel = null;
		}
	}

	private void perform_shutdown() {
		boolean shutdown_complete = true;
		switch (mc_state) {
		case MC_HC_CONNECTED:
		case MC_ACTIVE:
			for (final Host host : hosts) {
				if (host.hc_state != hc_state_enum.HC_DOWN) {
					send_exit_hc(host);
					host.hc_state = hc_state_enum.HC_EXITING;
					shutdown_complete = false;
				}
			}
			// no break
		case MC_LISTENING:
		case MC_LISTENING_CONFIGURED:
			shutdown_server();
			// don't call status_change() if shutdown is complete
			// it will be called from thread_main() later
			if (shutdown_complete) {
				mc_state = mcStateEnum.MC_INACTIVE;
			} else {
				mc_state = mcStateEnum.MC_SHUTDOWN;
				status_change();
			}
			break;
		default:
			fatal_error("MainController::perform_shutdown: called in wrong state.");
		}
	}

	private void send_exit_hc(final Host hc) {
		final Text_Buf text_buf = new Text_Buf();
		text_buf.push_int(MSG_EXIT_HC);
		send_message(hc.socket, text_buf);
	}

	private void clean_up() {
		shutdown_server();

		while (!unknown_connections.isEmpty()) {
			final unknown_connection connection = unknown_connections.getFirst();
			close_unknown_connection(connection);
		}

		destroy_all_components();

		for (final Host hc : hosts) {
			close_hc_connection(hc);
			hc.components.clear();
			hc.allowed_components.clear();
		}

		hosts.clear();
		config_str = null;

		// FIXME debugger support

		while (!timers.isEmpty()) {
			cancel_timer(timers.peek());
		}

		modules.clear();

		try {
			mc_selector.close();
		} catch (IOException e) {
			// FIXME handle
		}

		channel_table.clear();
		mc_state = mcStateEnum.MC_INACTIVE;
	}
}
