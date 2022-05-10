COMPILE_DEPS = CORE_DEPS + JACKSON + KRYO + REST + CLI + [
    "//apps/np/app:onos-apps-np-app",
    "//apps/np/api:onos-apps-np-api",
]
    #+ [
    #"//core/api:onos-api",
    #"//cli:onos-cli",
    ##"//core/store/serializers:onos-core-serializers",
    ##"//core/store/primitives:onos-core-primitives",
    #"//utils/osgi:onlab-osgi",
    #"//utils/misc:onlab-misc",
    #"@com_google_guava_guava//jar",
    #"@org_osgi_service_component_annotations//jar",

    #"//apps/np/app:onos-apps-np-app",
    #"//apps/np/api:onos-apps-np-api",
    #"//core/store/serializers:onos-core-serializers",
#]

osgi_jar_with_tests(
    deps = COMPILE_DEPS,
)

onos_app(
    #app_name = "org.nifwd",
    category = "Utility",
    description = "NIFWD Testing",
    #included_bundles = BUNDLES,
    #required_apps = ["org.onosproject.hostprovider"],
    title = "Netviews Intent Reactive Forwarding",
    #required_apps = [
    #    "org.onosproject.np.impl",
    #],
    url = "http://onosproject.org",
)
