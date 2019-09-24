package org.webswing.directdraw.util;

import java.util.HashMap;

import org.webswing.directdraw.model.DrawConstant;

public class LRUDrawConstantPoolCache {

    private HashMap<DrawConstant<?>, DoubleLinkedListNode> map = new HashMap<DrawConstant<?>, DoubleLinkedListNode>();
    private DoubleLinkedListNode head;
    private DoubleLinkedListNode end;
    private int capacity;
    private final int idOffset;
    private final int maxSize;
    // reserve zero id for null constants
    private int nextId = 1;

    public LRUDrawConstantPoolCache(int capacity,int idOffset, int maxSize) {
        this.capacity = capacity;
        this.idOffset = idOffset;
        this.maxSize = maxSize;
    }

    public int getCapacity() {
        return capacity;
    }

    public void increaseCapacity(){
        capacity*=2;
        if(capacity>maxSize){
            throw new RuntimeException("Directdraw: can not process more then "+maxSize+" constants in single frame. Check rendering of swing application.");
        }
    }

    public synchronized boolean contains(DrawConstant<?> constant) {
        return map.containsKey(constant);
    }

    public DrawConstant<?> getOrAdd(DrawConstant<?> constant) {
        if (map.containsKey(constant)) {
            DoubleLinkedListNode oldNode = map.get(constant);
            oldNode.remove();
            oldNode.makeHead();
            return oldNode.getVal();
        } else {
            DoubleLinkedListNode newNode = new DoubleLinkedListNode(constant);
            newNode.makeHead();
            if (nextId >= capacity) {
                // remove oldest node
                int evictedId = map.remove(end.getVal()).getVal().getId();
                end.remove();
                newNode.setId(evictedId);
            }else{
                newNode.setId((nextId++) + idOffset);
            }
            map.put(constant, newNode);
            return constant;
        }
    }

	private class DoubleLinkedListNode {

        private DrawConstant<?> val;
        private DoubleLinkedListNode pre;
        private DoubleLinkedListNode next;

        public DoubleLinkedListNode(DrawConstant<?> value) {
            val = value;
        }

        public void setId(int id) {
            val.setId(id);
        }

        public DrawConstant<?> getVal() {
            return val;
        }

        public void remove() {
            if (pre != null) {
                pre.next = next;
            } else {
                head = next;
            }
            if (next != null) {
                next.pre = pre;
            } else {
                end = pre;
            }
        }

        public void makeHead() {
            next = head;
            pre = null;
            if (head != null) {
                head.pre = this;
            }
            head = this;
            if (end == null) {
                end = this;
            }
        }
    }
}
