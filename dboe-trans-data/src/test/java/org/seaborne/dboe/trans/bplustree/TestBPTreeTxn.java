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

package org.seaborne.dboe.trans.bplustree;

import com.hp.hpl.jena.query.ReadWrite ;

import org.junit.Assert ;
import org.junit.Test ;
import org.seaborne.dboe.base.file.Location ;
import org.seaborne.dboe.index.test.IndexTestLib ;
import org.seaborne.dboe.test.RecordLib ;
import org.seaborne.dboe.transaction.Transactional ;
import org.seaborne.dboe.transaction.Txn ;
import org.seaborne.dboe.transaction.txn.TransactionalBase ;
import org.seaborne.dboe.transaction.txn.TransactionalComponent ;
import org.seaborne.dboe.transaction.txn.journal.Journal ;

/** Test of B+Tree and transactions */ 
public class TestBPTreeTxn extends Assert {
    
//    static {
//        BPT.forcePromoteModes = true ;
//        BPT.promoteDuplicateNodes = true ;
//        BPT.promoteDuplicateRecords = true ;
//    }
    
    static BPlusTree createBPTree() { 
        return BPlusTreeFactory.makeMem(2, 2, RecordLib.TestRecordLength, 0) ;
    }
    
    static Transactional transactional(TransactionalComponent ... components) {
        Journal journal = Journal.create(Location.mem()) ;
        Transactional holder = new TransactionalBase(journal, components) ;
        return holder ;
    }
    
    // Commit
    @Test public void bptree_txn_01() {
        BPlusTree bpt = createBPTree() ;
        int outerRootIdx1 = bpt.getRootId() ;
        Transactional thing = transactional(bpt) ;
        Txn.executeWrite(thing, () -> { 
            IndexTestLib.add(bpt, 1, 2, 3, 4) ;
        } ); 
        int outerRootIdx2 = bpt.getRootId() ;
        assertNotEquals("After txn", outerRootIdx1, outerRootIdx2); 
    }
 
    // Commit - only the first changes the root.
    @Test public void bptree_txn_02() {
        BPlusTree bpt = createBPTree() ;
        int outerRootIdx1 = bpt.getRootId() ;
        Transactional thing = transactional(bpt) ;
        Txn.executeWrite(thing, () -> { 
            int rootIdx1 = bpt.getRootId() ;
            assertEquals("Inside txn (1)", outerRootIdx1, rootIdx1);
            IndexTestLib.add(bpt, 1) ;
            int rootIdx2 = bpt.getRootId() ;
            assertNotEquals("Inside txn (2)", rootIdx1, rootIdx2);
            IndexTestLib.add(bpt, 2, 3, 4) ;
            int rootIdx3 = bpt.getRootId() ;
            assertEquals("Inside txn (3)", rootIdx2, rootIdx3);
        } ) ; 
        int outerRootIdx2 = bpt.getRootId() ;
        assertNotEquals("After txn", outerRootIdx1, outerRootIdx2); 
    }

    // Abort
    @Test public void bptree_txn_03() {
        BPlusTree bpt = createBPTree() ;
        int outerRootIdx1 = bpt.getRootId() ;
        Transactional thing = transactional(bpt) ;
        thing.begin(ReadWrite.WRITE);
        IndexTestLib.add(bpt, 1, 2, 3, 4) ;
        thing.abort() ;
        thing.end() ;
        int outerRootIdx2 = bpt.getRootId() ;
        assertEquals("After txn", outerRootIdx1, outerRootIdx2); 
    }
    
    // Two transactions
    @Test public void bptree_txn_04() {
        BPlusTree bpt = createBPTree() ;
        int outerRootIdx1 = bpt.getRootId() ;
        Transactional thing = transactional(bpt) ;
        Txn.executeWrite(thing, () -> { 
            IndexTestLib.add(bpt, 1, 2, 3, 4) ;
        } ); 
        int outerRootIdx2 = bpt.getRootId() ;
        assertNotEquals("After txn(1)", outerRootIdx1, outerRootIdx2); 
        Txn.executeWrite(thing, () -> { 
            IndexTestLib.add(bpt, 5, 6) ;
        } ); 
        int outerRootIdx3 = bpt.getRootId() ;
        assertNotEquals("After txn (2)", outerRootIdx1, outerRootIdx3); 
        assertNotEquals("After txn (3)", outerRootIdx2, outerRootIdx3); 
    }
    
    // Two transactions, second an insert no-op.
    // Relies on all blocks not being full and so not being
    // split on the way down due to the early split algorithm. 
    @Test public void bptree_txn_05() {
        BPlusTree bpt = createBPTree() ;
        int outerRootIdx1 = bpt.getRootId() ;
        Transactional thing = transactional(bpt) ;
        Txn.executeWrite(thing, () -> { 
            IndexTestLib.add(bpt, 1, 2, 3) ;
        } ); 
        int outerRootIdx2 = bpt.getRootId() ;
        assertNotEquals("After txn(1)", outerRootIdx1, outerRootIdx2); 
        Txn.executeWrite(thing, () -> { 
            IndexTestLib.add(bpt, 1, 2) ;
        } ); 
        int outerRootIdx3 = bpt.getRootId() ;
        assertNotEquals("After txn (2)", outerRootIdx1, outerRootIdx3); 
        assertEquals("After txn (3)", outerRootIdx2, outerRootIdx3); 
    }

    // Two transactions, second a delete no-op.
    // Relies on all blocks not being min0size so not rebalanced.
    @Test public void bptree_txn_06() {
        BPlusTree bpt = createBPTree() ;
        int outerRootIdx1 = bpt.getRootId() ;
        Transactional thing = transactional(bpt) ;
        Txn.executeWrite(thing, () -> { 
            IndexTestLib.add(bpt, 1, 2, 3) ;
        } ); 
        int outerRootIdx2 = bpt.getRootId() ;
        assertNotEquals("After txn(1)", outerRootIdx1, outerRootIdx2); 
        Txn.executeWrite(thing, () -> { 
            IndexTestLib.delete(bpt, 5, 6) ;
        } ); 
        int outerRootIdx3 = bpt.getRootId() ;
        assertNotEquals("After txn (2)", outerRootIdx1, outerRootIdx3); 
        assertEquals("After txn (3)", outerRootIdx2, outerRootIdx3); 
    }
    
}