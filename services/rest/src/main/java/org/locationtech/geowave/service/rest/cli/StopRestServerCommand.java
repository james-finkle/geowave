/**
 * Copyright (c) 2013-2019 Contributors to the Eclipse Foundation
 *
 * <p> See the NOTICE file distributed with this work for additional information regarding copyright
 * ownership. All rights reserved. This program and the accompanying materials are made available
 * under the terms of the Apache License, Version 2.0 which accompanies this distribution and is
 * available at http://www.apache.org/licenses/LICENSE-2.0.txt
 */
package org.locationtech.geowave.service.rest.cli;

import org.locationtech.geowave.core.cli.annotations.GeowaveOperation;
import org.locationtech.geowave.core.cli.api.Command;
import org.locationtech.geowave.core.cli.api.DefaultOperation;
import org.locationtech.geowave.core.cli.api.OperationParams;
import org.locationtech.geowave.service.rest.ApiRestletApplication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.beust.jcommander.Parameters;

@GeowaveOperation(name = "stop", parentOperation = RestSection.class)
@Parameters(commandDescription = "Terminates the GeoWave gRPC server")
public class StopRestServerCommand extends DefaultOperation implements Command {
  private static final Logger LOGGER = LoggerFactory.getLogger(StartRestServerCommand.class);

  /** Prep the driver & run the operation. */
  @Override
  public void execute(final OperationParams params) {

    LOGGER.info("Stopping GeoWave Rest server");
    try {
      ApiRestletApplication.getInstance().shutDown();
    } catch (Exception e) {
      // TODO Auto-generated catch block
      LOGGER.error("Exception encountered stopping rest server", e);
    }
  }
}
