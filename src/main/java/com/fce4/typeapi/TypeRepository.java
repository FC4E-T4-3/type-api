package com.fce4.typeapi;

import java.util.HashMap;

import org.springframework.stereotype.Component;

@Component
public class TypeRepository{

    private HashMap<String, TypeEntity> typeCache;

    public TypeRepository(){
        this.typeCache = new HashMap<String, TypeEntity>();
    }

    public void save(TypeEntity typeEntity){
        typeCache.put(typeEntity.getPid(), typeEntity);
    }

    public TypeEntity get(String pid){
        return typeCache.get(pid);
    }

    public boolean hasPid(String pid) {
        return typeCache.containsKey(pid);
    }
}
