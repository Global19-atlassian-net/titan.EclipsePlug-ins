###############################################################################
# Copyright (c) 2000-2018 Ericsson Telecom AB
# All rights reserved. This program and the accompanying materials
# are made available under the terms of the Eclipse Public License v2.0
# which accompanies this distribution, and is available at
# https://www.eclipse.org/org/documents/epl-2.0/EPL-2.0.html
###############################################################################
The the build can be configured in the 'build_config.xml' file.
By default the build logs can be found in the result/logs folder.
The output of the whole build process can be written to a log file using the -l option.
The following properties needs to be updated:
	version
	ECLIPSE_HOME
	cvsRoot

Usage:
	ant -f build_main.xml <target>

Build the update site:
	ant -f build_main.xml updatesite.{experimental | FOA | release}

Build a plugin:
	ant -f build_main.xml {common | designer | executor | help | logviewer}.plugin

clean:
	ant -f build_main.xml clean-all  
