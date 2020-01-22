package us.tfg.p2pmessenger.model;

/**
 * Created by FPiriz on 21/8/17.
 */

public class Parametros {
    public static final String parametros =
            "# this file holds the default values for pastry and it's applications\n"+
            "# you do not need to modify the default.params file to override these values\n"+
            "# instead you can use your own params file to set values to override the \n"+
            "# defaults.  You can specify this file by constructing your\n"+
            "# rice.environment.Environment() with the filename you wish to use\n"+
            "# typically, you will want to be able to pass this file name from the command \n"+
            "# line\n"+
            "\n"+
            "# max number of handles stored per routing table entry\n"+
            "pastry_rtMax = 1\n"+
            "pastry_rtBaseBitLength = 4\n"+
            "\n"+
            "# leafset size\n"+
            "pastry_lSetSize = 24\n"+
            "\n"+
            "# maintenance frequencies\n"+
            "pastry_leafSetMaintFreq = 60\n"+
            "pastry_routeSetMaintFreq = 900\n"+
            "\n"+
            "# drop the envelope if pastry is not ready\n"+
            "pastry_messageDispatch_bufferIfNotReady = false\n"+
            "\n"+
            "# number of messages to buffer while an app hasn't yet been registered\n"+
            "pastry_messageDispatch_bufferSize = 32\n"+
            "\n"+
            "# FP 2.1 uses the new transport layer\n"+
            "transport_wire_datagram_receive_buffer_size = 131072\n"+
            "transport_wire_datagram_send_buffer_size = 65536\n"+
            "transport_epoch_max_num_addresses = 2\n"+
            "transport_sr_max_num_hops = 5\n"+
            "\n"+
            "# proximity neighbor selection\n"+
            "transport_use_pns = true\n"+
            "\n"+
            "# number of rows in the routing table to consider during PNS\n"+
            "# valid values are ALL, or a number\n"+
            "pns_num_rows_to_use = 10\n"+
            "\n"+
            "# commonapi testing parameters\n"+
            "\n"+
            "# direct or socket\n"+
            "commonapi_testing_exit_on_failure = true\n"+
            "commonapi_testing_protocol = direct\n"+
            "commonapi_testing_startPort = 5009\n"+
            "commonapi_testing_num_nodes = 10\n"+
            "# set this to specify the bootstrap node\n"+
            "#commonapi_testing_bootstrap = localhost:5009\n"+
            "\n"+
            "# random number generator's seed, \"CLOCK\" uses the current clock time\n"+
            "random_seed = CLOCK\n"+
            "\n"+
            "# sphere, euclidean or gt-itm\n"+
            "direct_simulator_topology = sphere\n"+
            "# -1 starts the simulation with the current time\n"+
            "direct_simulator_start_time = -1\n"+
            "#pastry_direct_use_own_random = true\n"+
            "#pastry_periodic_leafset_protocol_use_own_random = true\n"+
            "pastry_direct_gtitm_matrix_file=GNPINPUT\n"+
            "# the number of stubs in your network\n"+
            "pastry_direct_gtitm_max_overlay_size=1000\n"+
            "# the number of virtual nodes at each stub: this allows you to simulate multiple \"LANs\" and allows cheeper scaling\n"+
            "pastry_direct_gtitm_nodes_per_stub=1\n"+
            "# the factor to multiply your file by to reach millis.  Set this to 0.001 if your file is in microseconds.  Set this to 1000 if your file is in seconds.\n"+
            "pastry_direct_gtitm_delay_factor=1.0\n"+
            "#millis of the maximum network delay for the generated network topologies\n"+
            "pastry_direct_max_diameter=200\n"+
            "pastry_direct_min_delay=2\n"+
            "#setting this to false will use the old protocols which are about 200 times as fast, but may cause routing inconsistency in a real network.  Probably won't in a simulator because it will never be incorrect about liveness\n"+
            "pastry_direct_guarantee_consistency=true\n"+
            "\n"+
            "# rice.pastry.socket parameters\n"+
            "# tells the factory you intend to use multiple nodes\n"+
            "# this causes the logger to prepend all entries with the nodeid\n"+
            "pastry_factory_multipleNodes = true\n"+
            "pastry_factory_selectorPerNode = false\n"+
            "pastry_factory_processorPerNode = false\n"+
            "# number of bootstap nodehandles to fetch in parallel\n"+
            "pastry_factory_bootsInParallel = 1\n"+
            "\n"+
            "# the maximum size of a envelope\n"+
            "pastry_socket_reader_selector_deserialization_max_size = 1000000\n"+
            "# the maximum number of outgoing messages to queue when a socket is slower than the number of messages you are queuing\n"+
            "pastry_socket_writer_max_queue_length = 30\n"+
            "pastry_socket_writer_max_msg_size = 20480\n"+
            "pastry_socket_repeater_buffer_size = 65536\n"+
            "pastry_socket_pingmanager_smallPings=true\n"+
            "pastry_socket_pingmanager_datagram_receive_buffer_size = 131072\n"+
            "pastry_socket_pingmanager_datagram_send_buffer_size = 65536\n"+
            "# the time before it will retry a route that was already found dead\n"+
            "pastry_socket_srm_check_dead_throttle = 300000\n"+
            "pastry_socket_srm_proximity_timeout = 3600000\n"+
            "pastry_socket_srm_ping_throttle = 30000\n"+
            "pastry_socket_srm_default_rto = 3000\n"+
            "pastry_socket_srm_rto_ubound = 10000\n"+
            "pastry_socket_srm_rto_lbound = 50\n"+
            "pastry_socket_srm_gain_h = 0.25\n"+
            "pastry_socket_srm_gain_g = 0.125\n"+
            "pastry_socket_scm_max_open_sockets = 300\n"+
            "pastry_socket_scm_max_open_source_routes = 30\n"+
            "# the maximum number of source routes to attempt, setting this to 0 will \n"+
            "# effectively eliminate source route attempts\n"+
            "# setting higher than the leafset does no good, it will be bounded by the leafset\n"+
            "# a larger number tries more source routes, which could give you a more accurate \n"+
            "# determination, however, is more likely to lead to congestion collapse\n"+
            "pastry_socket_srm_num_source_route_attempts = 8\n"+
            "pastry_socket_scm_socket_buffer_size = 32768\n"+
            "# this parameter is multiplied by the exponential backoff when doing a liveness check so the first will be 800, then 1600, then 3200 etc...\n"+
            "pastry_socket_scm_ping_delay = 800\n"+
            "# adds some fuzziness to the pings to help prevent congestion collapse, so this will make the ping be advanced or delayed by this factor\n"+
            "pastry_socket_scm_ping_jitter = 0.1\n"+
            "# how many pings until we call the node faulty\n"+
            "pastry_socket_scm_num_ping_tries = 5\n"+
            "pastry_socket_scm_write_wait_time = 30000\n"+
            "pastry_socket_scm_backoff_initial = 250\n"+
            "pastry_socket_scm_backoff_limit = 5\n"+
            "pastry_socket_pingmanager_testSourceRouting = false\n"+
            "pastry_socket_increment_port_after_construction = true\n"+
            "# if you want to allow connection to 127.0.0.1, set this to true\n"+
            "pastry_socket_allow_loopback = false\n"+
            "# these params will be used if the computer attempts to bind to the loopback address, they will open a socket to this address/port to identify which network adapter to bind to\n"+
            "pastry_socket_known_network_address = yahoo.com\n"+
            "pastry_socket_known_network_address_port = 80\n"+
            "pastry_socket_use_own_random = true\n"+
            "pastry_socket_random_seed = clock\n"+
            "# force the node to be a seed node\n"+
            "rice_socket_seed = false\n"+
            "\n"+
            "# the parameter simulates some nodes being firewalled, base on rendezvous_test_num_firewalled\n"+
            "rendezvous_test_firewall = false\n"+
            "# probabilistic fraction of firewalled nodes\n"+
            "rendezvous_test_num_firewalled = 0.3\n"+
            "# don't firewall the first node, useful for testing\n"+
            "rendezvous_test_makes_bootstrap = false\n"+
            "\n"+
            "# FP 2.1 uses the new transport layer\n"+
            "transport_wire_datagram_receive_buffer_size = 131072\n"+
            "transport_wire_datagram_send_buffer_size = 65536\n"+
            "\n"+
            "# NAT/UPnP settings\n"+
            "nat_network_prefixes = 127.0.0.1;10.;192.168.\n"+
            "# Enable and set this if you have already set up port forwarding and know the external address\n"+
            "#external_address = 123.45.67.89:1234\n"+
            "#enable this if you set up port forwarding (on the same port), but you don't \n"+
            "#know the external address and you don't have UPnP enabled\n"+
            "#this is useful for a firwall w/o UPnP support, and your IP address isn't static\n"+
            "probe_for_external_address = false\n"+
            "# values how to probe\n"+
            "pastry_proxy_connectivity_timeout = 15000 \n"+
            "pastry_proxy_connectivity_tries = 3\n"+
            "# possible values: always, never, prefix (prefix is if the localAddress matches any of the nat_network_prefixes\n"+
            "# whether to search for a nat using UPnP (default: prefix)\n"+
            "nat_search_policy = prefix\n"+
            "# whether to verify connectivity (default: boot)\n"+
            "firewall_test_policy = boot\n"+
            "# policy for setting port forwarding the state of the firewall if there is already a conflicting rule: overwrite, fail (throw exception), change (use different port) \n"+
            "# you may want to set this to overwrite or fail on the bootstrap nodes, but most freepastry applications can run on any available port, so the default is change\n"+
            "nat_state_policy = change\n"+
            "# the name of the application in the firewall, set this if you want your application to have a more specific name\n"+
            "nat_app_name = freepastry\n"+
            "# how long to wait for responses from the firewall, in millis\n"+
            "nat_discovery_timeout = 5000\n"+
            "# how many searches to try to find a free firewall port\n"+
            "nat_find_port_max_tries = 10\n"+
            "# uncomment this to use UPnP NAT port forwarding, you need to include in the classpath: commons-jxpath-1.1.jar:commons-logging.jar:sbbi-upnplib-xxx.jar\n"+
            "nat_handler_class = rice.pastry.socket.nat.sbbi.SBBINatHandler\n"+
            "# hairpinning: \n"+
            "# default \"prefix\" requires more bandwidth if you are behind a NAT.  It enables multiple IP \n"+
            "# addresses in the NodeHandle if you are behind a NAT.  These are usually the internet routable address, \n"+
            "# and the LAN address (usually 192.168.x.x)\n"+
            "# you can set this to never if any of the following conditions hold:\n"+
            "#  a) you are the only FreePastry node behind this address\n"+
            "#  b) you firewall supports hairpinning see\n"+
            "# http://scm.sipfoundry.org/rep/ietf-drafts/behave/draft-ietf-behave-nat-udp-03.html#rfc.section.6\n"+
            "nat_nodehandle_multiaddress = prefix\n"+
            "\n"+
            "# if we are not scheduled for time on cpu in this time, we setReady(false)\n"+
            "# otherwise there could be envelope inconsistency, because\n"+
            "# neighbors may believe us to be dead.  Note that it is critical\n"+
            "# to consider the amount of time it takes the transport layer to find a \n"+
            "# node faulty before setting this parameter, this parameter should be\n"+
            "# less than the minimum time required to find a node faulty\n"+
            "pastry_protocol_consistentJoin_max_time_to_be_scheduled = 15000\n"+
            "\n"+
            "# in case messages are dropped or something, how often it will retry to \n"+
            "# send the consistent join envelope, to get verification from the entire\n"+
            "# leafset\n"+
            "pastry_protocol_consistentJoin_retry_interval = 30000\n"+
            "# parameter to control how long dead nodes are retained in the \"failed set\" in\n"+
            "# CJP (see ConsistentJoinProtocol ctor) (15 minutes)\n"+
            "pastry_protocol_consistentJoin_failedRetentionTime = 900000\n"+
            "# how often to cleanup the failed set (5 mins) (see ConsistentJoinProtocol ctor)\n"+
            "pastry_protocol_consistentJoin_cleanup_interval = 300000\n"+
            "# the maximum number of entries to send in the failed set, only sends the most \n"+
            "recent detected failures (see ConsistentJoinProtocol ctor)\n"+
            "pastry_protocol_consistentJoin_maxFailedToSend = 20\n"+
            "\n"+
            "# how often we send/expect to be sent updates\n"+
            "pastry_protocol_periodicLeafSet_ping_neighbor_period = 20000\n"+
            "pastry_protocol_periodicLeafSet_lease_period = 30000\n"+
            "\n"+
            "# what the grace period is to receive a periodic update, before checking \n"+
            "# liveness\n"+
            "pastry_protocol_periodicLeafSet_request_lease_throttle = 10000\n"+
            "\n"+
            "# how many entries are kept in the partition handler's table\n"+
            "partition_handler_max_history_size=20\n"+
            "# how long entries in the partition handler's table are kept\n"+
            "# 90 minutes\n"+
            "partition_handler_max_history_age=5400000\n"+
            "# what fraction of the time a bootstrap host is checked\n"+
            "partition_handler_bootstrap_check_rate=0.05\n"+
            "# how often to run the partition handler\n"+
            "# 5 minutes\n"+
            "partition_handler_check_interval=300000\n"+
            "\n"+
            "# the version number of the RouteMessage to transmit (it can receive anything that it knows how to)\n"+
            "# this is useful if you need to migrate an older ring\n"+
            "# you can change this value in realtime, so, you can start at 0 and issue a command to update it to 1\n"+
            "pastry_protocol_router_routeMsgVersion = 1\n"+
            "\n"+
            "# should usually be equal to the pastry_rtBaseBitLength\n"+
            "p2p_splitStream_stripeBaseBitLength = 4\n"+
            "p2p_splitStream_policy_default_maximum_children = 24\n"+
            "p2p_splitStream_stripe_max_failed_subscription = 5\n"+
            "p2p_splitStream_stripe_max_failed_subscription_retry_delay = 1000\n"+
            "\n"+
            "#multiring\n"+
            "p2p_multiring_base = 2\n"+
            "\n"+
            "#past\n"+
            "p2p_past_messageTimeout = 30000\n"+
            "p2p_past_successfulInsertThreshold = 0.5\n"+
            "\n"+
            "#replication\n"+
            "\n"+
            "# fetch delay is the delay between fetching successive keys\n"+
            "p2p_replication_manager_fetch_delay = 500\n"+
            "# the timeout delay is how long we take before we time out fetching a key\n"+
            "p2p_replication_manager_timeout_delay = 20000\n"+
            "# this is the number of keys to delete when we detect a change in the replica set\n"+
            "p2p_replication_manager_num_delete_at_once = 100\n"+
            "# this is how often replication will wake up and do maintainence; 10 mins\n"+
            "p2p_replication_maintenance_interval = 600000 \n"+
            "# the maximum number of keys replication will try to exchange in a maintainence envelope\n"+
            "p2p_replication_max_keys_in_message = 1000\n"+
            "\n"+
            "#scribe\n"+
            "p2p_scribe_maintenance_interval = 180000\n"+
            "#time for a subscribe fail to be thrown (in millis)\n"+
            "p2p_scribe_message_timeout = 15000\n"+
            "\n"+
            "#util\n"+
            "p2p_util_encryptedOutputStream_buffer = 32678\n"+
            "\n"+
            "#aggregation\n"+
            "p2p_aggregation_logStatistics = true\n"+
            "p2p_aggregation_flushDelayAfterJoin = 30000\n"+
            "#5 MINS\n"+
            "p2p_aggregation_flushStressInterval = 300000 \n"+
            "#5 MINS\n"+
            "p2p_aggregation_flushInterval = 300000 \n"+
            "#1024*1024\n"+
            "p2p_aggregation_maxAggregateSize = 1048576 \n"+
            "p2p_aggregation_maxObjectsInAggregate = 25\n"+
            "p2p_aggregation_maxAggregatesPerRun = 2\n"+
            "p2p_aggregation_addMissingAfterRefresh = true\n"+
            "p2p_aggregation_maxReaggregationPerRefresh = 100\n"+
            "p2p_aggregation_nominalReferenceCount = 2\n"+
            "p2p_aggregation_maxPointersPerAggregate = 100\n"+
            "#14 DAYS\n"+
            "p2p_aggregation_pointerArrayLifetime = 1209600000\n"+
            "#1 DAY \n"+
            "p2p_aggregation_aggregateGracePeriod = 86400000\n"+
            "#15 MINS \n"+
            "p2p_aggregation_aggrRefreshInterval = 900000 \n"+
            "p2p_aggregation_aggrRefreshDelayAfterJoin = 70000\n"+
            "#3 DAYS\n"+
            "p2p_aggregation_expirationRenewThreshold = 259200000 \n"+
            "p2p_aggregation_monitorEnabled = false\n"+
            "#15 MINS\n"+
            "p2p_aggregation_monitorRefreshInterval = 900000 \n"+
            "#5 MINS\n"+
            "p2p_aggregation_consolidationDelayAfterJoin = 300000 \n"+
            "#15 MINS\n"+
            "p2p_aggregation_consolidationInterval = 900000 \n"+
            "#14 DAYS\n"+
            "p2p_aggregation_consolidationThreshold = 1209600000 \n"+
            "p2p_aggregation_consolidationMinObjectsInAggregate = 20\n"+
            "p2p_aggregation_consolidationMinComponentsAlive = 0.8\n"+
            "p2p_aggregation_reconstructionMaxConcurrentLookups = 10\n"+
            "p2p_aggregation_aggregateLogEnabled = true\n"+
            "#1 HOUR\n"+
            "p2p_aggregation_statsGranularity = 3600000\n"+
            "#3 WEEKS \n"+
            "p2p_aggregation_statsRange = 1814400000 \n"+
            "p2p_aggregation_statsInterval = 60000\n"+
            "p2p_aggregation_jitterRange = 0.1\n"+
            "\n"+
            "# glacier\n"+
            "p2p_glacier_logStatistics = true\n"+
            "p2p_glacier_faultInjectionEnabled = false\n"+
            "p2p_glacier_insertTimeout = 30000\n"+
            "p2p_glacier_minFragmentsAfterInsert = 3.0\n"+
            "p2p_glacier_refreshTimeout = 30000\n"+
            "p2p_glacier_expireNeighborsDelayAfterJoin = 30000\n"+
            "#5 MINS\n"+
            "p2p_glacier_expireNeighborsInterval = 300000 \n"+
            "#5 DAYS\n"+
            "p2p_glacier_neighborTimeout = 432000000 \n"+
            "p2p_glacier_syncDelayAfterJoin = 30000\n"+
            "#5 MINS\n"+
            "p2p_glacier_syncMinRemainingLifetime = 300000 \n"+
            "#insertTimeout\n"+
            "p2p_glacier_syncMinQuietTime = 30000 \n"+
            "p2p_glacier_syncBloomFilterNumHashes = 3\n"+
            "p2p_glacier_syncBloomFilterBitsPerKey = 4\n"+
            "p2p_glacier_syncPartnersPerTrial = 1\n"+
            "#1 HOUR\n"+
            "p2p_glacier_syncInterval = 3600000 \n"+
            "#3 MINUTES\n"+
            "p2p_glacier_syncRetryInterval = 180000 \n"+
            "p2p_glacier_syncMaxFragments = 100\n"+
            "p2p_glacier_fragmentRequestMaxAttempts = 0\n"+
            "p2p_glacier_fragmentRequestTimeoutDefault = 10000\n"+
            "p2p_glacier_fragmentRequestTimeoutMin = 10000\n"+
            "p2p_glacier_fragmentRequestTimeoutMax = 60000\n"+
            "p2p_glacier_fragmentRequestTimeoutDecrement = 1000\n"+
            "p2p_glacier_manifestRequestTimeout = 10000\n"+
            "p2p_glacier_manifestRequestInitialBurst = 3\n"+
            "p2p_glacier_manifestRequestRetryBurst = 5\n"+
            "p2p_glacier_manifestAggregationFactor = 5\n"+
            "#3 MINUTES\n"+
            "p2p_glacier_overallRestoreTimeout = 180000 \n"+
            "p2p_glacier_handoffDelayAfterJoin = 45000\n"+
            "#4 MINUTES\n"+
            "p2p_glacier_handoffInterval = 240000 \n"+
            "p2p_glacier_handoffMaxFragments = 10\n"+
            "#10 MINUTES\n"+
            "p2p_glacier_garbageCollectionInterval = 600000 \n"+
            "p2p_glacier_garbageCollectionMaxFragmentsPerRun = 100\n"+
            "#10 MINUTES\n"+
            "p2p_glacier_localScanInterval = 600000 \n"+
            "p2p_glacier_localScanMaxFragmentsPerRun = 20\n"+
            "p2p_glacier_restoreMaxRequestFactor = 4.0\n"+
            "p2p_glacier_restoreMaxBoosts = 2\n"+
            "p2p_glacier_rateLimitedCheckInterval = 30000\n"+
            "p2p_glacier_rateLimitedRequestsPerSecond = 3\n"+
            "p2p_glacier_enableBulkRefresh = true\n"+
            "p2p_glacier_bulkRefreshProbeInterval = 3000\n"+
            "p2p_glacier_bulkRefreshMaxProbeFactor = 3.0\n"+
            "p2p_glacier_bulkRefreshManifestInterval = 30000\n"+
            "p2p_glacier_bulkRefreshManifestAggregationFactor = 20\n"+
            "p2p_glacier_bulkRefreshPatchAggregationFactor = 50\n"+
            "#3 MINUTES\n"+
            "p2p_glacier_bulkRefreshPatchInterval = 180000 \n"+
            "p2p_glacier_bulkRefreshPatchRetries = 2\n"+
            "p2p_glacier_bucketTokensPerSecond = 100000\n"+
            "p2p_glacier_bucketMaxBurstSize = 200000\n"+
            "p2p_glacier_jitterRange = 0.1\n"+
            "#1 MINUTE\n"+
            "p2p_glacier_statisticsReportInterval = 60000 \n"+
            "p2p_glacier_maxActiveRestores = 3\n"+
            "\n"+
            "#transport layer testing params\n"+
            "org.mpisws.p2p.testing.transportlayer.replay.Recorder_printlog = true\n"+
            "\n"+
            "# logging\n"+
            "#default log level\n"+
            "loglevel = CONFIG\n"+
            "\n"+
            "\n"+
            "#example of enabling logging on the endpoint:\n"+
            "#rice.p2p.scribe@ScribeRegrTest-endpoint_loglevel = INFO\n"+
            "rice.p2p.scribe@myScribeInstance= WARNING\n"+
            "logging_date_format = yyyyMMdd.HHmmss.SSS\n"+
            "logging_enable=true\n"+
            "\n"+
            "# 24 hours\n"+
            "log_rotate_interval = 86400000\n"+
            "# the name of the active log file, and the filename prefix of rotated log\n"+
            "log_rotate_filename = freepastry.log\n"+
            "# the format of the date for the rotating log\n"+
            "log_rotating_date_format = yyyyMMdd.HHmmss.SSS\n"+
            "\n"+
            "# true will tell the environment to ues the FileLogManager\n"+
            "environment_logToFile = false\n"+
            "# the prefix for the log files (otherwise will be named after the nodeId)\n"+
            "fileLogManager_filePrefix = \n"+
            "# the suffix for the log files\n"+
            "fileLogManager_fileSuffix = .log\n"+
            "# wether to keep the line prefix (declaring the node id) for each line of the log\n"+
            "fileLogManager_keepLinePrefix = false\n"+
            "fileLogManager_multipleFiles = true\n"+
            "fileLogManager_defaultFileName = main\n"+
            "\n"+
            "# false = append true = overwrite\n"+
            "fileLogManager_overwrite_existing_log_file = false\n"+
            "\n"+
            "# the amount of time the LookupService tutorial app will wait before timing out\n"+
            "# in milliseconds, default is 30 seconds\n"+
            "lookup_service.timeout = 30000\n"+
            "# how long to wait before the first retry\n"+
            "lookup_service.firstTimeout = 500\n";
}
