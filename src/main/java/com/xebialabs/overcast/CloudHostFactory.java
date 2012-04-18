/* License added by: GRADLE-LICENSE-PLUGIN
 *
 * Copyright 2008-2012 XebiaLabs
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.xebialabs.overcast;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.xebialabs.overcast.OvercastProperties.getOvercastProperty;
import static com.xebialabs.overcast.OvercastProperties.getRequiredOvercastProperty;
import static com.xebialabs.overcast.OvercastProperties.parsePortsProperty;


public class CloudHostFactory {

	public static final String HOSTNAME_PROPERTY_SUFFIX = ".hostname";

	public static final String TUNNEL_USERNAME_PROPERTY_SUFFIX = ".tunnel.username";
	public static final String TUNNEL_PASSWORD_PROPERTY_SUFFIX = ".tunnel" + OvercastProperties.PASSWORD_PROPERTY_SUFFIX;
	public static final String TUNNEL_PORTS_PROPERTY_SUFFIX = ".tunnel.ports";

	// The field logger needs to be defined up here so that the static
	// initialized below can use the logger
	public static Logger logger = LoggerFactory.getLogger(CloudHostFactory.class);

	public static CloudHost getCloudHostWithNoTeardown(String hostLabel) {
		return getCloudHost(hostLabel, true);
	}

	public static CloudHost getCloudHost(String hostLabel) {
		return getCloudHost(hostLabel, false);
	}

	private static CloudHost getCloudHost(String hostLabel, boolean disableEc2) {
		CloudHost host = createCloudHost(hostLabel, disableEc2);
		return wrapCloudHost(hostLabel, host);
	}

	protected static CloudHost createCloudHost(String label, boolean disableEc2) {
		String hostName = getOvercastProperty(label + HOSTNAME_PROPERTY_SUFFIX);
		if (hostName != null) {
			logger.info("Using existing host for {}", label);
			return new ExistingCloudHost(label);
		}

		String amiId = getOvercastProperty(label + Ec2CloudHost.AMI_ID_PROPERTY_SUFFIX);
		if (amiId != null) {
			if (disableEc2) {
				throw new IllegalStateException("Only an AMI ID (" + amiId + ") has been specified for host label " + label
						+ ", but EC2 hosts are not available.");
			}
			logger.info("Using Amazon EC2 for {}", label);
			return new Ec2CloudHost(label, amiId);
		}

		throw new IllegalStateException("Neither a hostname (" + hostName + ") nor an AMI id (" + amiId + ") have been specified for host label " + label);
	}

	private static CloudHost wrapCloudHost(String label, CloudHost actualHost) {
		String tunnelUsername = getOvercastProperty(label + TUNNEL_USERNAME_PROPERTY_SUFFIX);
		if (tunnelUsername == null) {
			return actualHost;
		}

		logger.info("Starting SSH tunnels for {}", label);

		String tunnelPassword = getRequiredOvercastProperty(label + TUNNEL_PASSWORD_PROPERTY_SUFFIX);
		String ports = getRequiredOvercastProperty(label + TUNNEL_PORTS_PROPERTY_SUFFIX);
		Map<Integer, Integer> portForwardMap = parsePortsProperty(ports);
		return new TunneledCloudHost(actualHost, tunnelUsername, tunnelPassword, portForwardMap);
	}

}
