package org.webswing.directdraw.util;

import java.util.HashMap;

import org.webswing.directdraw.model.DrawConstant;

public class LRUDrawConstantPoolCache {

    private HashMap<DrawConstant<?>, DoubleLinkedListNode> map = new HashMap<DrawConstant<?>, DoubleLinkedListNode>();
    private DoubleLinkedListNode head;
    private DoubleLinkedListNode end;
    private int capacity;
    private final int idOffset;
    // reserve zero id for null constants
    private int nextId = 1;

    public LRUDrawConstantPoolCache(int capacity,int idOffset) {
        this.capacity = capacity;
        this.idOffset = idOffset;
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
