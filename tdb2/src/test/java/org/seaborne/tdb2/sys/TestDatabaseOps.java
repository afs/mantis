/*
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *  See the NOTICE file distributed with this work for additional
 *  information regarding copyright ownership.
 */

package org.seaborne.tdb2.sys;

import static org.junit.Assert.assertEquals ;
import static org.junit.Assert.assertFalse ;
import static org.junit.Assert.assertNotEquals ;
import static org.junit.Assert.assertTrue ;

import org.apache.commons.io.FileUtils ;
import org.apache.jena.atlas.lib.FileOps ;
import org.apache.jena.riot.RDFDataMgr ;
import org.apache.jena.sparql.core.DatasetGraph ;
import org.apache.jena.sparql.core.Quad ;
import org.apache.jena.sparql.sse.SSE ;
import org.junit.After ;
import org.junit.Before ;
import org.junit.Test ;
import org.seaborne.dboe.base.file.Location ;
import org.seaborne.dboe.jenax.Txn ;
import org.seaborne.tdb2.ConfigTest ;
import org.seaborne.tdb2.DatabaseMgr ;
import org.seaborne.tdb2.store.DatasetGraphSwitchable ;
import org.seaborne.tdb2.store.DatasetGraphTDB ;

public class TestDatabaseOps
{
    static String DIRx = ConfigTest.getCleanDir() ;
    static Location DIR = Location.create(DIRx);
    
    static Quad quad1 = SSE.parseQuad("(_ _:a <p> 1)") ;
    static Quad quad2 = SSE.parseQuad("(_ <s> <p> 1)") ;
    
    @Before
    public void before() {
        FileUtils.deleteQuietly(IOX.asFile(DIR));
        FileOps.ensureDir(DIR.getDirectoryPath());
    }

    @After  
    public void after() {
        TDBInternal.reset();
        FileUtils.deleteQuietly(IOX.asFile(DIR));
    }

    @Test public void compact_1() {
        DatasetGraph dsg = DatabaseMgr.connectDatasetGraph(DIR);
        DatasetGraphSwitchable dsgs = (DatasetGraphSwitchable)dsg;
        DatasetGraph dsg1 = dsgs.get();
        Location loc1 = ((DatasetGraphTDB)dsg1).getLocation();
        
        Txn.executeWrite(dsg, ()-> {
            dsg.add(quad1) ;
            dsg.add(quad2) ;
        }) ;
        DatabaseMgr.compact(dsg);
        
        assertFalse(StoreConnection.isSetup(loc1));

        DatasetGraph dsg2 = dsgs.get();
        Location loc2 = ((DatasetGraphTDB)dsg2).getLocation();

        assertNotEquals(dsg1, dsg2);
        assertNotEquals(loc1, loc2);

        Txn.executeRead(dsg, ()-> {
            assertTrue(dsg.contains(quad1)) ;
            assertTrue(dsg.contains(quad2)) ;
        }) ;
        
        // dsg1 was closed and expelled. We must carefully reopen its storage only.
        
        DatasetGraph dsgOld = StoreConnection.connectCreate(loc1).getDatasetGraph();
        
        Txn.executeWrite(dsgOld, ()->dsgOld.delete(quad1));
        Txn.executeRead(dsg,     ()->assertTrue(dsg.contains(quad1)) );
        Txn.executeRead(dsg2,    ()->assertTrue(dsg2.contains(quad1)) ) ;
    }

//    @Test public void compact_2() {
//    }

    @Test public void backup_1() {
        DatasetGraph dsg = DatabaseMgr.connectDatasetGraph(DIR);
        Txn.executeWrite(dsg, ()-> {
            dsg.add(quad1) ;
            dsg.add(quad2) ;
        }) ;
        String file1 = DatabaseMgr.backup(dsg);
        DatasetGraph dsg2 = RDFDataMgr.loadDatasetGraph(file1);
        Txn.executeRead(dsg, ()-> {
            assertTrue(dsg.contains(quad2)) ;
            assertEquals(2, dsg.getDefaultGraph().size());
            assertTrue(dsg2.getDefaultGraph().isIsomorphicWith(dsg.getDefaultGraph()));
        }) ;
        String file2 = DatabaseMgr.backup(dsg);
        assertNotEquals(file1, file2);
    }
    
}