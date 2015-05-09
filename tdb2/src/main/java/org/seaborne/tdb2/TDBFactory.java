/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.seaborne.tdb2;

import org.apache.jena.query.Dataset ;
import org.apache.jena.query.DatasetFactory ;
import org.apache.jena.sparql.core.DatasetGraph ;
import org.seaborne.dboe.base.file.Location ;
import org.seaborne.tdb2.sys.StoreConnection ;


/** Public factory for creating objects datasets backed by TDB storage */
public class TDBFactory
{
    private TDBFactory() {} 
    
    public static DatasetGraph createDatasetGraph(Location location) {
        StoreConnection sConn = StoreConnection.getCreate(location) ;
        return sConn.getDatasetGraphTDB() ; 
    }

    public static Dataset createDataset(Location location) {
        DatasetGraph dsg = createDatasetGraph(location) ;
        return DatasetFactory.create(dsg) ;
    }

    public static DatasetGraph createDatasetGraph(String location) {
        return createDatasetGraph(Location.create(location)) ;
    }

    public static Dataset createDataset(String location) {
        return createDataset(Location.create(location)) ;    }

    public static DatasetGraph createDatasetGraph() {
        return createDatasetGraph(Location.mem()) ;
    }

    public static Dataset createDataset() {
        return createDataset(Location.mem()) ;
    }

}
//    
//    /** Read the file and assembler a dataset */
//    public static Dataset assembleDataset(String assemblerFile) {
//        return (Dataset)AssemblerUtils.build(assemblerFile, VocabTDB.tDatasetTDB) ;
//    }
//    
//    /** Create or connect to a TDB-backed dataset */ 
//    public static Dataset createDataset(String dir)
//    { return createDataset(Location.create(dir)) ; }
//
//    /** Create or connect to a TDB-backed dataset */ 
//    public static Dataset createDataset(Location location)
//    { return createDataset(createDatasetGraph(location)) ; }
//
//    /** Create or connect to a TDB dataset backed by an in-memory block manager. For testing.*/ 
//    public static Dataset createDataset()
//    { return createDataset(createDatasetGraph()) ; }
//
//    /** Create a dataset around a DatasetGraphTDB */ 
//    private static Dataset createDataset(DatasetGraph datasetGraph)
//    { return DatasetFactory.create(datasetGraph) ; }
//    
//    /** Create or connect to a TDB-backed dataset (graph-level) */
//    public static DatasetGraph createDatasetGraph(String directory)
//    { return createDatasetGraph(Location.create(directory)) ; }
//
//    /** Create or connect to a TDB-backed dataset (graph-level) */
//    public static DatasetGraph createDatasetGraph(Location location)
//    { return _createDatasetGraph(location) ; }
//
//    /** Create a TDB-backed dataset (graph-level) in memory (for testing) */
//    public static DatasetGraph createDatasetGraph() {
//        return _createDatasetGraph() ;
//    }
//    
//    /** Release from the JVM. All caching is lost. */
//    public static void release(Dataset dataset) {
//        _release(location(dataset)) ;
//    }
//    
//    /** Release from the JVM.  All caching is lost. */
//    public static void release(DatasetGraph dataset) {
//        _release(location(dataset)) ;
//    }
//    
//    private static DatasetGraph _createDatasetGraph(Location location) {
//        return TDBMaker.createDatasetGraphTransaction(location) ;
//    }
//
//    private static DatasetGraph _createDatasetGraph() {
//        return TDBMaker.createDatasetGraphTransaction() ;
//    }
//    
//    private static void _release(Location location) {
//        if ( location == null )
//            return ;
//        TDBMaker.releaseLocation(location) ;
//    }
//
//    /** Return the location of a dataset if it is backed by TDB, else null */ 
//    public static boolean isBackedByTDB(Dataset dataset) {
//        DatasetGraph dsg = dataset.asDatasetGraph() ;
//        return isBackedByTDB(dsg) ;
//    }
//    
//    /** Return the location of a dataset if it is backed by TDB, else null */ 
//    public static boolean isBackedByTDB(DatasetGraph datasetGraph) {
//        if ( datasetGraph instanceof DatasetGraphTransaction )
//            // The swicthing "connection" for TDB 
//            return true ;
//        if ( datasetGraph instanceof DatasetGraphTDB )
//            // A transaction or the base storage.
//            return true ;
//        return false ;
//    }
//
//    /** Return the location of a dataset if it is backed by TDB, else null */
//    public static Location location(Dataset dataset) {
//        DatasetGraph dsg = dataset.asDatasetGraph() ;
//        return location(dsg) ;
//    }
//
//    /** Return the location of a DatasetGraph if it is backed by TDB, else null */
//    public static Location location(DatasetGraph datasetGraph) {
//        if ( datasetGraph instanceof DatasetGraphTDB )
//            return ((DatasetGraphTDB)datasetGraph).getLocation() ;
//        if ( datasetGraph instanceof DatasetGraphTransaction )
//            return ((DatasetGraphTransaction)datasetGraph).getLocation() ;
//        return null ;
//    }
//
//    /** Set the {@link StoreParams} for specific Location.
//     *  This call must only be called before a dataset from Location
//     *  is created. This operation should be used with care; bad choices
//     *  of {@link StoreParams} can reduce performance.
//     *  
//     *  <a href="http://jena.apache.org/documentation/tdb/store-paramters.html"
//     *  >See documentation</a>.
//     *  
//     *  @param location  The persistent storage location
//     *  @param params  StoreParams to use
//     *  @throws IllegalStateException If the dataset has already been setup.
//     */
//    public static void setup(Location location, StoreParams params) {
//        StoreConnection sConn = StoreConnection.getExisting(location) ;
//        if ( sConn != null )
//            throw new IllegalStateException("Location is already active") ;
//        StoreConnection.make(location, params) ;
//    }