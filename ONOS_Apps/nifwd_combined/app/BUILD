COMPILE_DEPS = CORE_DEPS + JACKSON + KRYO + REST + CLI + [
    "//core/store/serializers:onos-core-serializers",
    "@junit//jar",
    #"@gson//:gson-api",
    "@com_google_code_gson_gson//jar",
    "@junit_jupiter_api//jar",
    "@snakeyaml//jar",
    #"@gson//:gson-api",
    "@jgrapht_core//jar",
]

osgi_jar_with_tests(
    api_description = "API for Netviews Policy COMBINED",
    api_package = "org.onosproject.nifwd_combined.impl",
    api_title = "Netviews Policy COMBINED",
    api_version = "1.0",
    #karaf_command_packages = ["org.onosproject.dhcp.cli"],
    #test_deps = TEST_ADAPTERS,
    #web_context = "/onos/dhcp",
    deps = COMPILE_DEPS,
)
