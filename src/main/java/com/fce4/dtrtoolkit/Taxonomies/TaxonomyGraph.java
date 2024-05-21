package com.fce4.dtrtoolkit.Taxonomies;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Set;

import com.fce4.dtrtoolkit.Entities.TaxonomyEntity;
import org.springframework.stereotype.Component;

@Component
public class TaxonomyGraph {
    HashMap<String, TaxonomyEntity> taxonomy = new HashMap<String, TaxonomyEntity>();

    public void addNode(TaxonomyEntity t){
        taxonomy.put(t.getPid(), t);
    }

    public void clear(){
        this.taxonomy.clear();
    }

    public void generateRelations(){
        for(TaxonomyEntity t : taxonomy.values()){
            for(String parent : t.getParentsString()){
                t.addParent(taxonomy.get(parent));
                taxonomy.get(parent).addChild(t);
            }
        }
    }

    public LinkedHashMap<String,Object> getSubtree(String pid) throws Exception{
        LinkedHashMap<String,Object> tree = new LinkedHashMap<String,Object>();
        ArrayList<TaxonomyEntity> currentChildren = new ArrayList<TaxonomyEntity>();
        tree.put(pid, get(pid).serializeSearch());
        for(TaxonomyEntity t : get(pid).getChildren()){
            currentChildren.add(t);
        }
        while(currentChildren.size()>0){
            TaxonomyEntity tmp = currentChildren.get(0);
            tree.put(tmp.getPid(), tmp.serializeSearch());
            for(TaxonomyEntity t : tmp.getChildren()){
                currentChildren.add(t);
            }
            currentChildren.remove(0);
        }
        return tree;
    }

    public Set<String> getSubtreePIDs(String pid) throws Exception {
        return getSubtree(pid).keySet();
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