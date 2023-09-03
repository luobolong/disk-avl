package com.luobo;

import java.io.IOException;
import java.io.RandomAccessFile;

/**
 * <h1>Disk-Based AVL Tree</h1>
 * Represents a disk-based binary search tree where each node occupies 16 bytes in a disk file.
 * <p>
 * The 16 bytes for each node are divided as follows:
 * </p>
 * <pre>
 * +-------------------+--------------------+----------------+---------------+
 * | Left Child Offset | Right Child Offset | Node Height    | Node Data     |
 * |      (4 bytes)    |       (4 bytes)    |   (4 bytes)    |   (4 bytes)   |
 * +-------------------+--------------------+----------------+---------------+
 * </pre>
 *
 * <p>
 * The "offset" is the starting address of a node in the disk file. All nodes are stored in the disk sequentially
 * based on the insertion order. The first byte in the disk file is reserved to store the root node's offset,
 * enabling us to locate the root node.
 * </p>
 *
 * <p>
 * This implementation allows for a maximum file size of 4 GB.
 * </p>
 */
public class DiskAvlTree {
    /**
     * Size of each node in bytes.
     */
    private static final int NODE_SIZE = 16;

    /**
     * Byte offset for the left child within each node.
     */
    private static final int LEFT_CHILD_OFFSET = 0;

    /**
     * Byte offset for the right child within each node.
     */
    private static final int RIGHT_CHILD_OFFSET = 4;

    /**
     * Byte offset for the height information within each node.
     */
    private static final int NODE_HEIGHT_OFFSET = 8;

    /**
     * Byte offset for the data within each node.
     */
    private static final int DATA_OFFSET = 12;

    /**
     * The disk file where the AVL tree is stored.
     */
    private final RandomAccessFile file;

    /**
     * Creates a DiskAvlTree object and initializes the disk file.
     * If the disk file is empty, it initializes the root node's offset to 0.
     *
     * @param filename The name of the file to be used for storing the AVL tree.
     * @throws IOException If an IO error occurs during file operations.
     */
    public DiskAvlTree(String filename) throws IOException {
        file = new RandomAccessFile(filename, "rw");
        if (file.length() == 0) {
            // Initialize the root node's offset to 0.
            file.writeInt(0);
        }
    }

    /**
     * Inserts a node with the given data into the tree.
     *
     * @param data The data to be inserted.
     * @throws IOException If an IO error occurs during file operations.
     */
    public void insert(int data) throws IOException {
        int root = getRoot();
        int newRoot = insert(root, data);
        if (newRoot != root) {
            setRoot(newRoot);
        }
    }

    /**
     * Searches for a node containing the given data in the tree.
     *
     * @param data The data to be searched for.
     * @return True if a node containing the given data is found, otherwise false.
     * @throws IOException If an IO error occurs during file operations.
     */
    public boolean search(int data) throws IOException {
        int root = getRoot();
        return search(root, data);
    }

    /**
     * Deletes a node containing the given data from the tree.
     *
     * @param data The data to be deleted.
     * @throws IOException If an IO error occurs during file operations.
     */
    public void delete(int data) throws IOException {
        int root = getRoot();
        int newRoot = delete(root, data);
        if (root != newRoot) {
            setRoot(newRoot);
        }
    }

    /**
     * Recursively inserts a node with the given data into the tree.
     *
     * @param offset      The offset of the parent node in the disk file.
     * @param newNodeData The data for the new node.
     * @return The offset of the inserted (or updated) node.
     * @throws IOException If an IO error occurs during file operations.
     */
    private int insert(int offset, int newNodeData) throws IOException {
        // If offset is 0, the node is null; allocate 16 bytes at the end of the file.
        if (offset == 0) {
            long fileLength = file.length();
            int newNode = (int) fileLength;
            file.setLength(fileLength + NODE_SIZE);
            setNodeData(newNode, newNodeData);
            return newNode;
        }

        // Retrieve the left and right child offsets and the node data for the current node.
        int left = getLeft(offset);
        int right = getRight(offset);
        int nodeData = getNodeData(offset);
        if (newNodeData < nodeData) {
            // If the new node data is smaller, recursively insert into the left subtree.
            int newLeft = insert(left, newNodeData);
            if (newLeft != left) {
                setLeft(offset, newLeft);
            }
        } else if (newNodeData > nodeData) {
            // If the new node data is larger, recursively insert into the right subtree.
            int newRight = insert(right, newNodeData);
            if (newRight != right) {
                setRight(offset, newRight);
            }
        } else {
            // If the data is the same, throw a duplicate data exception.
            throw new RuntimeException("Duplicate data!");
        }

        // Re-balance the tree and return the new node offset.
        return rebalance(offset);
    }

    /**
     * Recursively searches for a node with the given data.
     *
     * @param offset The offset of the parent node in the disk file.
     * @param data   The data to search for.
     * @return True if a node with the given data is found; otherwise, false.
     * @throws IOException If an IO error occurs during file operations.
     */
    private boolean search(int offset, int data) throws IOException {
        // If the offset is 0, the node is null; return false.
        if (offset == 0) {
            return false;
        }

        // Retrieve the node data for the current node.
        int nodeData = getNodeData(offset);
        if (nodeData == data) {
            return true;
        } else if (nodeData > data) {
            // If the current node's data is larger, recursively search the left subtree.
            int left = getLeft(offset);
            return search(left, data);
        } else {
            // If the current node's data is smaller, recursively search the right subtree.
            int right = getRight(offset);
            return search(right, data);
        }
    }

    /**
     * Recursively deletes a node with the given data from the tree.
     *
     * @param offset The offset of the parent node in the disk file.
     * @param data   The data to be deleted.
     * @return The offset of the new root node after deletion.
     * @throws IOException If an IO error occurs during file operations.
     */
    private int delete(int offset, int data) throws IOException {
        // If the offset is 0, the node is null; return 0.
        if (offset == 0) {
            return 0;
        }

        // Retrieve the node data for the current node.
        int nodeData = getNodeData(offset);

        if (data < nodeData) {
            // If the data to delete is smaller, recursively delete from the left subtree.
            int left = getLeft(offset);
            int newLeft = delete(left, data);
            if (left != newLeft) {
                setLeft(offset, newLeft);
            }
        } else if (data > nodeData) {
            // If the data to delete is larger, recursively delete from the right subtree.
            int right = getRight(offset);
            int newRight = delete(right, data);
            if (right != newRight) {
                setRight(offset, newRight);
            }
        } else {
            // If the data matches, proceed with the deletion.
            int left = getLeft(offset);
            int right = getRight(offset);

            if (left == 0 && right == 0) {
                // If the node is a leaf node, free the node and return 0.
                freeNode(offset);
                return 0;
            } else if (left == 0) {
                // If the node has only a right child, free the node and return the right child's offset.
                freeNode(offset);
                return right;
            } else if (right == 0) {
                // If the node has only a left child, free the node and return the left child's offset.
                freeNode(offset);
                return left;
            } else {
                // If the node has both left and right children, find the in-order successor and replace the node.
                int successor = findSuccessor(right);
                int successorData = getNodeData(successor);
                setNodeData(offset, successorData);
                int newRight = delete(right, successorData);
                if (newRight != right) {
                    setRight(offset, newRight);
                }
            }
        }
        return offset; // Return the offset of the new root node after deletion.
    }

    /**
     * Retrieves the root node's offset from the first 4 bytes of the disk file.
     *
     * @return The offset of the root node.
     * @throws IOException If an IO error occurs during file read.
     */
    private int getRoot() throws IOException {
        file.seek(0);
        return file.readInt();
    }

    /**
     * Sets the root node's offset at the beginning of the disk file.
     *
     * @param rootOffset The new offset for the root node.
     * @throws IOException If an IO error occurs during file write.
     */
    private void setRoot(int rootOffset) throws IOException {
        file.seek(0);
        file.writeInt(rootOffset);
    }

    /**
     * Retrieves the left child's offset for a given node offset.
     *
     * @param offset The offset of the node in question.
     * @return The offset of the left child node.
     * @throws IOException          If an IO error occurs during file read.
     * @throws NullPointerException If the node offset is null.
     */
    private int getLeft(int offset) throws IOException {
        if (offset == 0) {
            throw new NullPointerException("Null pointer!");
        }
        file.seek(offset + LEFT_CHILD_OFFSET);
        return file.readInt();
    }

    /**
     * Sets the left child's offset for a given node.
     *
     * @param offset  The offset of the node to modify.
     * @param newLeft The new offset for the left child node.
     * @throws IOException If an IO error occurs during file write.
     */
    private void setLeft(int offset, int newLeft) throws IOException {
        file.seek(offset + LEFT_CHILD_OFFSET);
        file.writeInt(newLeft);
    }

    /**
     * Retrieves the right child's offset for a given node offset.
     *
     * @param offset The offset of the node in question.
     * @return The offset of the right child node.
     * @throws IOException          If an IO error occurs during file read.
     * @throws NullPointerException If the node offset is null.
     */
    private int getRight(int offset) throws IOException {
        if (offset == 0) {
            throw new NullPointerException("Null pointer!");
        }
        file.seek(offset + RIGHT_CHILD_OFFSET);
        return file.readInt();
    }

    /**
     * Sets the right child's offset for a given node.
     *
     * @param offset   The offset of the node to modify.
     * @param newRight The new offset for the right child node.
     * @throws IOException If an IO error occurs during file write.
     */
    private void setRight(int offset, int newRight) throws IOException {
        file.seek(offset + RIGHT_CHILD_OFFSET);
        file.writeInt(newRight);
    }

    /**
     * Retrieves the height of a node given its offset. Returns -1 if the node is null.
     *
     * @param offset The offset of the node in question.
     * @return The height of the node or -1 if the node is null.
     * @throws IOException If an IO error occurs during file read.
     */
    private int getNodeHeight(int offset) throws IOException {
        if (offset == 0) {
            return -1;
        }
        file.seek(offset + NODE_HEIGHT_OFFSET);
        return file.readInt();
    }

    /**
     * Updates the height of a node based on its children's heights.
     *
     * @param offset The offset of the node whose height is to be updated.
     * @throws IOException If an IO error occurs during file write.
     */
    private void updateNodeHeight(int offset) throws IOException {
        int height = 1 + Math.max(getNodeHeight(getLeft(offset)), getNodeHeight(getRight(offset)));
        file.seek(offset + NODE_HEIGHT_OFFSET);
        file.writeInt(height);
    }

    /**
     * Retrieves the data stored in a node given its offset.
     *
     * @param offset The offset of the node in question.
     * @return The data stored in the node.
     * @throws IOException      If an IO error occurs during file read.
     * @throws RuntimeException If the node offset is null.
     */
    private int getNodeData(int offset) throws IOException {
        if (offset == 0) {
            throw new RuntimeException("Null pointer!!");
        }
        file.seek(offset + DATA_OFFSET);
        return file.readInt();
    }

    /**
     * Sets the data for a node given its offset.
     *
     * @param offset The offset of the node to modify.
     * @param data   The new data for the node.
     * @throws IOException If an IO error occurs during file write.
     */
    private void setNodeData(int offset, int data) throws IOException {
        file.seek(offset + DATA_OFFSET);
        file.writeInt(data);
    }

    /**
     * Calculates the balance factor of a node, which is the height of the right subtree
     * minus the height of the left subtree. If the node is null, returns 0.
     *
     * @param offset The offset of the node whose balance factor is to be calculated.
     * @return The balance factor of the node.
     * @throws IOException If an IO error occurs during file read.
     */
    private int getBalance(int offset) throws IOException {
        if (offset == 0) {
            return 0;
        }
        return getNodeHeight(getRight(offset)) - getNodeHeight(getLeft(offset));
    }

    /**
     * Performs a right rotation on the subtree rooted at the given offset.
     * This operation is used to balance the AVL tree.
     *
     * @param offset The offset of the root node of the subtree to be rotated.
     * @return The new root node offset after the rotation.
     * @throws IOException If an IO error occurs during file read/write.
     */
    private int rotateRight(int offset) throws IOException {
        // 1. Save the left child
        int left = getLeft(offset);
        // 2. Save the right child of the left child
        int right = getRight(left);
        // 3. Set the right child of the left child to be the root
        setRight(left, offset);
        // 4. Set the left child of the root to be the right child of the left child
        setLeft(offset, right);
        // 5. First, update the height of the root
        updateNodeHeight(offset);
        // 6. Then, update the height of the left child
        updateNodeHeight(left);
        // 7. Return the left child as the new root
        return left;
    }

    /**
     * Performs a left rotation on the subtree rooted at the given offset.
     * This operation is used to balance the AVL tree.
     *
     * @param offset The offset of the root node of the subtree to be rotated.
     * @return The new root node offset after the rotation.
     * @throws IOException If an IO error occurs during file read/write.
     */
    private int rotateLeft(int offset) throws IOException {
        // 1. Save the right child
        int right = getRight(offset);
        // 2. Save the left child of the right child
        int left = getLeft(offset);
        // 3. Set the left child of the right child to be the root
        setLeft(right, offset);
        // 4. Set the right child of the root to be the left child of the right child
        setRight(offset, left);
        // 5. First, update the height of the root
        updateNodeHeight(offset);
        // 6. Then, update the height of the right child
        updateNodeHeight(right);
        // 7. Return the right child as the new root
        return right;
    }

    /**
     * Rebalances the AVL tree rooted at the given offset.
     * It checks the balance factor and performs the appropriate rotations.
     *
     * @param offset The offset of the node that serves as the root of the subtree to be rebalanced.
     * @return The new root node offset after rebalancing.
     * @throws IOException If an IO error occurs during file read/write.
     */
    private int rebalance(int offset) throws IOException {
        // Update the height of the current node
        updateNodeHeight(offset);
        // Calculate the balance factor
        int balance = getBalance(offset);

        if (balance > 1) {
            int right = getRight(offset);
            if (getNodeHeight(getRight(right)) < getNodeHeight(getLeft(right))) {
                // RL case
                int rightOfRight = getRight(offset);
                setRight(offset, rotateRight(rightOfRight));
            }
            offset = rotateLeft(offset);
        } else if (balance < -1) {
            int left = getLeft(offset);
            if (getNodeHeight(getLeft(left)) < getNodeHeight(getRight(left))) {
                // LR case
                int leftOfLeft = getLeft(offset);
                setLeft(offset, rotateLeft(leftOfLeft));
            }
            offset = rotateRight(offset);
        }
        return offset;
    }

    /**
     * Marks a node as deleted by writing zeros into its block in the file.
     * This operation is done to indicate that the node at the given offset is no longer in use.
     *
     * @param offset The file offset of the node to be deleted.
     * @throws IOException If an IO error occurs during file write.
     */
    private void freeNode(int offset) throws IOException {
        file.seek(offset);
        // Write 0 to all bytes in the node's block to mark it as free
        for (int i = 0; i < NODE_SIZE; i++) {
            file.write(0);
        }
    }

    /**
     * Finds the in-order successor of the node with the given offset.
     * The in-order successor is the node with the smallest value greater than the value of the given node.
     * In an AVL tree, this is the leftmost child of the right subtree.
     *
     * @param offset The file offset of the node whose in-order successor is to be found.
     * @return The file offset of the in-order successor.
     * @throws IOException If an IO error occurs during file read.
     */
    private int findSuccessor(int offset) throws IOException {
        // Go to the leftmost node in the right subtree
        int left = getLeft(offset);
        if (left == 0) {
            return offset;
        }
        return findSuccessor(left);
    }
}
