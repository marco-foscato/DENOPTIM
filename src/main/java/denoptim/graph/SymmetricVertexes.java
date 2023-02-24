package denoptim.graph;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

/**
 * A collection of {@link Vertex}s that are related by a relation that we call
 * "symmetry", even though this class does not define what such relation is.
 * Therefore, any elsewhere-defined relation may characterize the items
 * collected in instances of this class.
 */
public class SymmetricVertexes extends SymmetricSet<Vertex>
{
//------------------------------------------------------------------------------
    
    public SymmetricVertexes()
    {
        super();
    }
    
//------------------------------------------------------------------------------
    
    public SymmetricVertexes(List<Vertex> selVrtxs)
    {
        super();
        this.addAll(selVrtxs);
    }
  
//------------------------------------------------------------------------------

    public static class SymmetricVertexesSerializer
    implements JsonSerializer<SymmetricVertexes>
    {
        @Override
        public JsonElement serialize(SymmetricVertexes list, Type typeOfSrc,
              JsonSerializationContext context)
        {
            List<Integer> vertexIDs = new ArrayList<Integer>();
            for (Vertex v : list)
            {
                vertexIDs.add(v.getVertexId());
            }
            return context.serialize(vertexIDs);
        }
    }
    
    //NB: deserialization is done in the Graph deserializer

//------------------------------------------------------------------------------
    
}
