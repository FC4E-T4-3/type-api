package com.fce4.dtrtoolkit;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Logger;

import org.springframework.stereotype.Component;

@Component
public class TypeRepository{
   
    Logger logger = Logger.getLogger(TypeService.class.getName());

    private HashMap<String, TypeEntity> typeCache;

    public TypeRepository(){
        this.typeCache = new HashMap<String, TypeEntity>();
    }

    public void save(TypeEntity typeEntity){
        typeCache.put(typeEntity.getPid(), typeEntity);
    }

    public HashMap<String, TypeEntity> getCache(){
        return this.typeCache;
    }

    public void clear(){
        this.typeCache.clear();
    }

    public TypeEntity get(String pid){
        return typeCache.get(pid);
    }

    public boolean hasPid(String pid){
        return typeCache.containsKey(pid);
    }
}
