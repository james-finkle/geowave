/**
 * Copyright (c) 2013-2019 Contributors to the Eclipse Foundation
 *
 * <p> See the NOTICE file distributed with this work for additional information regarding copyright
 * ownership. All rights reserved. This program and the accompanying materials are made available
 * under the terms of the Apache License, Version 2.0 which accompanies this distribution and is
 * available at http://www.apache.org/licenses/LICENSE-2.0.txt
 */
package org.locationtech.geowave.format.landsat8;

import static org.junit.Assert.assertTrue;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import org.junit.BeforeClass;
import org.junit.Test;
import org.locationtech.geowave.adapter.raster.plugin.gdal.InstallGdal;
import org.locationtech.geowave.core.cli.api.OperationParams;
import org.locationtech.geowave.core.cli.operations.config.options.ConfigOptions;
import org.locationtech.geowave.core.cli.parser.ManualOperationParams;
import org.locationtech.geowave.core.geotime.ingest.SpatialDimensionalityTypeProvider;
import org.locationtech.geowave.core.store.CloseableIterator;
import org.locationtech.geowave.core.store.GeoWaveStoreFinder;
import org.locationtech.geowave.core.store.api.QueryBuilder;
import org.locationtech.geowave.core.store.cli.store.DataStorePluginOptions;
import org.locationtech.geowave.core.store.cli.store.StoreLoader;
import org.locationtech.geowave.core.store.index.IndexStore;
import org.locationtech.geowave.core.store.memory.MemoryStoreFactoryFamily;
import com.beust.jcommander.ParameterException;
import it.geosolutions.jaiext.JAIExt;

public class VectorIngestRunnerTest {

  @BeforeClass
  public static void setup() throws IOException {
    GeoWaveStoreFinder.getRegisteredStoreFactoryFamilies().put(
        "memory",
        new MemoryStoreFactoryFamily());

    InstallGdal.main(new String[] {System.getenv("GDAL_DIR")});
  }

  @Test
  public void testIngest() throws Exception {
    JAIExt.initJAIEXT();

    final Landsat8BasicCommandLineOptions analyzeOptions = new Landsat8BasicCommandLineOptions();
    analyzeOptions.setNBestScenes(1);
    analyzeOptions.setCqlFilter(
        "BBOX(shape,-76.6,42.34,-76.4,42.54) and band='BQA' and sizeMB < 1");
    analyzeOptions.setUseCachedScenes(true);
    analyzeOptions.setWorkspaceDir(Tests.WORKSPACE_DIR);

    final Landsat8DownloadCommandLineOptions downloadOptions =
        new Landsat8DownloadCommandLineOptions();
    downloadOptions.setOverwriteIfExists(false);

    final Landsat8RasterIngestCommandLineOptions ingestOptions =
        new Landsat8RasterIngestCommandLineOptions();
    ingestOptions.setRetainImages(true);
    ingestOptions.setCreatePyramid(true);
    ingestOptions.setCreateHistogram(true);
    final ManualOperationParams params = new ManualOperationParams();
    params.getContext().put(
        ConfigOptions.PROPERTIES_FILE_CONTEXT,
        new File(
            VectorIngestRunnerTest.class.getClassLoader().getResource(
                "geowave-config.properties").toURI()));
    createIndices(params);
    final VectorIngestRunner runner =
        new VectorIngestRunner(analyzeOptions, Arrays.asList("memorystore", "spatialindex"));
    runner.runInternal(params);
    try (CloseableIterator<Object> results =
        getStorePluginOptions(params).createDataStore().query(QueryBuilder.newBuilder().build())) {
      assertTrue("Store is not empty", results.hasNext());
    }

    // Not sure what assertions can be made about the index.
  }

  private DataStorePluginOptions getStorePluginOptions(final OperationParams params) {
    final File configFile = (File) params.getContext().get(ConfigOptions.PROPERTIES_FILE_CONTEXT);

    final StoreLoader inputStoreLoader = new StoreLoader("memorystore");
    if (!inputStoreLoader.loadFromConfig(configFile)) {
      throw new ParameterException("Cannot find store name: " + inputStoreLoader.getStoreName());
    }
    return inputStoreLoader.getDataStorePlugin();
  }

  private void createIndices(final OperationParams params) {
    IndexStore indexStore = getStorePluginOptions(params).createIndexStore();
    // Create the spatial index
    SpatialDimensionalityTypeProvider.SpatialIndexBuilder builder =
        new SpatialDimensionalityTypeProvider.SpatialIndexBuilder();
    builder.setName("spatialindex");
    builder.setNumPartitions(1);
    builder.setIncludeTimeInCommonIndexModel(false);
    indexStore.addIndex(builder.createIndex());
  }
}
