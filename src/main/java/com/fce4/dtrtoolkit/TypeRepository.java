package com.fce4.dtrtoolkit;

import java.util.ArrayList;
import java.util.HashMap;

import org.springframework.stereotype.Component;

@Component
public class TypeRepository{

    private HashMap<String, TypeEntity> typeCache;
    //Mapping tags to all related types
    private HashMap<String, ArrayList<String>> tagMap;

    public TypeRepository(){
        this.typeCache = new HashMap<String, TypeEntity>();
        this.tagMap = new HashMap<String, ArrayList<String>>();
    }

    public void save(TypeEntity typeEntity){
        typeCache.put(typeEntity.getPid(), typeEntity);
    }

    public TypeEntity get(String pid){
        return typeCache.get(pid);
    }

    public boolean hasPid(String pid){
        return typeCache.containsKey(pid);
    }

    public void addTypeTag(String tag, String pid){
        
        if(!tagMap.containsKey(tag)){
            tagMap.put(tag, new ArrayList<String>());
        }
        tagMap.get(tag).add(pid);
    }
}
