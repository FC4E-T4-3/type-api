package com.fce4.dtrtoolkit.Taxonomies;

import java.util.HashMap;

import org.springframework.stereotype.Component; 
import java.util.Objects;

@Component
public class TaxonomyGraph {
    HashMap<String, TaxonomyEntity> taxonomy = new HashMap<String, TaxonomyEntity>();

    public void addNode(TaxonomyEntity t){
        taxonomy.put(t.getPid(), t);
    }

    public void generateRelations(){
        for(TaxonomyEntity t : taxonomy.values()){
            for(String parent : t.getParentsString()){
                t.addParent(taxonomy.get(parent));
                taxonomy.get(parent).addChild(t);
            }
        }
    }

    public HashMap<String,TaxonomyEntity> getTaxonomy() {
        return this.taxonomy;
    }

    public TaxonomyEntity get(String pid){
        return taxonomy.get(pid);
    }

    @Override
    public String toString() {
        return "{" +
            " taxonomy='" + getTaxonomy() + "'" +
            "}";
    }
    
}
