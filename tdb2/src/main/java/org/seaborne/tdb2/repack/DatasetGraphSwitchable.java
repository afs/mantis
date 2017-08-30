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

package org.seaborne.tdb2.repack;

import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Node;
import org.apache.jena.shared.PrefixMapping;
import org.apache.jena.shared.impl.PrefixMappingImpl;
import org.apache.jena.sparql.core.*;
import org.seaborne.tdb2.store.DatasetGraphTDB;

final
public class DatasetGraphSwitchable extends DatasetGraphWrapper
{
    // QueryEngineFactoryWrapper has a QueryEngineFactory that is always loaded that
    // executes on the unwrapped DSG (recursively). Unwrapping is via getBase, calling
    // getWrapped() which is implemented with get().
    
//    static { 
//        // QueryEngineRegistry.addFactory(factory());
//    }
    
    private final AtomicReference<DatasetGraph> dsgx = new AtomicReference<>();
    // Null for in-memory datasets.
    private final Path basePath;
    
    public DatasetGraphSwitchable(Path base, DatasetGraph dsg) {
        // Don't use the slot in datasetGraphWrapper - use the AtomicReference
        super(null) ;
        dsgx.set(dsg);
        this.basePath = base;  
    }

    /** Is this {@code DatasetGraphSwitchable} just a holder for a {@code DatasetGraph}?
     *  If so, it does not have a location on disk.
     */
    public boolean hasContainerPath() { return basePath != null; } 
    
    public Path getContainerPath() { return basePath; }
    
    /** The dataset to use for redirection - can be overridden.
     *  It is also guaranteed that this is called only once per
     *  delegated call.  Changes to the wrapped object can be
     *  made based on that contract.
     */
    @Override
    public DatasetGraph get() { return dsgx.get(); }

    /** Set the base {@link DatasetGraph}.
     * Returns the old value.
     */ 
    public DatasetGraph set(DatasetGraph dsg) { 
        return dsgx.getAndSet(dsg);
    }
    
    /** If and only if the current value is the given old value, set the base {@link DatasetGraph}  
     * Returns true if a swap happened.
     */ 
    public boolean change(DatasetGraph oldDSG, DatasetGraph newDSG) { 
        return dsgx.compareAndSet(oldDSG, newDSG);
    }

    // Used directly, Does not get prefix mapping right.
    // GraphView has a private, empty prefix mapping

    Graph dft = new GraphView(this, Quad.defaultGraphNodeGenerated) {
        @Override
        protected PrefixMapping createPrefixMapping() {
            return prefixMapping(null);
        } 
    };
    
    @Override
    public Graph getDefaultGraph() {
        // Via the switchable.
        return new GraphView(this, Quad.defaultGraphNodeGenerated) {
            @Override
            protected PrefixMapping createPrefixMapping() {
                return prefixMapping(null);
            }
        };
    }
    
    @Override
    public Graph getGraph(Node gn) {
        if ( Quad.isUnionGraph(gn) )
            return GraphView.createUnionGraph(get());
        
        return new GraphView(this, gn) {
            @Override
            protected PrefixMapping createPrefixMapping() {
                return prefixMapping(gn);
            }
        };
    }

    // TDB2 specific.
    // Does not cope with blank nodes.
    // A PrefixMapping sending operations via the switchable.
    private PrefixMapping prefixMapping(Node graphName) {
        
        String gn = (graphName == null) ? "" : graphName.getURI(); 
        
        return new PrefixMappingImpl() {
            
            DatasetPrefixStorage dps() {
                return ((DatasetGraphTDB)dsgx.get()).getPrefixes();
            }
            
            Graph graph() {
                DatasetGraphTDB dsg = (DatasetGraphTDB)dsgx.get();
                if ( gn == null )
                    return dsg.getDefaultGraph();
                else
                    return dsg.getGraph(graphName);
            }
            
            PrefixMapping prefixMapping() {
                if ( gn == null )
                    return dps().getPrefixMapping();
                else
                    return dps().getPrefixMapping(gn); 
            }

            @Override
            protected void set(String prefix, String uri) {
                dps().insertPrefix(gn, prefix, uri);
                super.set(prefix, uri);
            }

            @Override
            protected String get(String prefix) {
                return dps().readPrefix(gn, prefix);
            }

            @Override
            protected void remove(String prefix) {
                dps().getPrefixMapping().removeNsPrefix(prefix);
                super.remove(prefix);
            }
            
            @Override
            public Map<String, String> getNsPrefixMap() {
                return prefixMapping().getNsPrefixMap();
                //return graph().getPrefixMapping().getNsPrefixMap();
            }
        };
    }
    
    //static { register() ; }
    
    
//    static QueryEngineFactory factory() {
//        return new QueryEngineFactory() {
//            @Override
//            public boolean accept(Op op, DatasetGraph dataset, Context context) {
//                DatasetGraphSwitchable dsg = extract(dataset) ;
//                if ( dsg == null ) return false;
//                QueryEngineFactory f = QueryEngineRegistry.findFactory(op, dsg.get(), context);
//                return f.accept(op, dataset, context);
//            }
//
//            @Override
//            public Plan create(Op op, DatasetGraph dataset, Binding inputBinding, Context context) {
//                DatasetGraphSwitchable dsg = extract(dataset) ;
//                if ( dsg == null ) return null;
//                QueryEngineFactory f = QueryEngineRegistry.findFactory(op, dsg.get(), context);
//                return f.create(op, dataset, inputBinding, context);
//            }
//
//            private DatasetGraphSwitchable extract(DatasetGraph dataset) {
//                if ( dataset instanceof DatasetGraphSwitchable )
//                    return (DatasetGraphSwitchable)dataset;
//                return null;
//            }
//
//            @Override
//            public boolean accept(Query query, DatasetGraph dataset, Context context) {
//                DatasetGraphSwitchable dsg = extract(dataset) ;
//                if ( dsg == null ) return false;
//                QueryEngineFactory f = QueryEngineRegistry.findFactory(query, dsg.get(), context);
//                return f.accept(query, dataset, context);
//            }
//
//            @Override
//            public Plan create(Query query, DatasetGraph dataset, Binding inputBinding, Context context) {
//                DatasetGraphSwitchable dsg = extract(dataset) ;
//                if ( dsg == null ) return null;
//                QueryEngineFactory f = QueryEngineRegistry.findFactory(query, dsg.get(), context);
//                return f.create(query, dataset, inputBinding, context);
//            }
//        };
//    }
}
