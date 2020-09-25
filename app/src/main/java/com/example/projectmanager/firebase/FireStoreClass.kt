package com.example.projectmanager.firebase

import android.app.Activity
import android.util.Log
import android.widget.Toast
import com.example.projectmanager.activities.*
import com.example.projectmanager.models.Board
import com.example.projectmanager.models.User
import com.example.projectmanager.utils.Constants
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

class FireStoreClass {

    private val mFireStore = FirebaseFirestore.getInstance()

    fun registerUser(activity: SignUpActivity, userInfo: User) {
        mFireStore.collection(Constants.USERS).document(getCurrentUserID()).set(userInfo, SetOptions.merge()).addOnSuccessListener {
            activity.userRegisteredSuccess()
        }.addOnFailureListener {
            e ->
            Log.e(activity.javaClass.simpleName, "Error registering user")
        }
    }

    fun createBoard(activity: CreateBoardActivity, board: Board) {
        mFireStore.collection(Constants.BOARDS).document().set(board, SetOptions.merge()).addOnSuccessListener {
            activity.boardCreatedSuccessfully()
            Log.i(activity.javaClass.simpleName, "Board Created Successfully")
            Toast.makeText(activity, "Board Created Successfully", Toast.LENGTH_SHORT).show()
        }.addOnFailureListener {
            e ->
            activity.hideProgressDialog()
            Log.e(activity.javaClass.simpleName, "Error creating Board", e)
        }
    }

    fun getCurrentUserID(): String {
        val currentUser = FirebaseAuth.getInstance().currentUser
        var currentUserID = ""
        if (currentUser != null) currentUserID = currentUser.uid
        return currentUserID
    }

    fun getBoardsList(activity: MainActivity) {
        mFireStore.collection(Constants.BOARDS).whereArrayContains(Constants.ASSIGNED_TO, getCurrentUserID()).get().addOnSuccessListener {
            document ->
            Log.i("Error 1: ", document.documents.toString())
            val boardsList : ArrayList<Board> = ArrayList()
            for (i in document.documents) {
                val board = i.toObject(Board::class.java)!!
                board.documentId = i.id
                boardsList.add(board)
            }

            activity.populateBoardsListToUI(boardsList)
        }.addOnFailureListener {
            e ->
            activity.hideProgressDialog()
            Log.e("Error 1: ", "Error retrieving a board", e)
        }
    }

    fun loadUserData(activity: Activity, readBoardsList: Boolean = false) {
        mFireStore.collection(Constants.USERS).document(getCurrentUserID()).get().addOnSuccessListener { document ->
            val loggedInUser = document.toObject(User::class.java)!!

            when (activity) {
                is SignInActivity -> activity.signInSuccess(loggedInUser)
                is MainActivity -> activity.updateNavigationUserDetails(loggedInUser, readBoardsList)
                is MyProfileActivity -> activity.setuserDataInUI(loggedInUser)
            }
        }.addOnFailureListener {
                e ->
            when (activity) {
                is SignInActivity -> activity.hideProgressDialog()
                is MainActivity -> activity.hideProgressDialog()
            }
            Log.e(activity.javaClass.simpleName, "Error writing document", e)
        }
    }

    fun updateUserData(activity: Activity, userHashMap: HashMap<String, Any>) {
        mFireStore.collection(Constants.USERS).document(getCurrentUserID()).update(userHashMap).addOnSuccessListener {
            Log.i(activity.javaClass.simpleName, "Profile Data updated successfully")
            Toast.makeText(activity, "Profile Updated Successfully!", Toast.LENGTH_SHORT).show()
            when (activity) {
                is MainActivity -> activity.tokenUpdateSuccess()
                is MyProfileActivity -> activity.profileUpdateSuccess()
            }
        }.addOnFailureListener {
            e ->
            when (activity) {
                is MainActivity -> activity.hideProgressDialog()
                is MyProfileActivity -> activity.hideProgressDialog()
            }
            Log.i(activity.javaClass.simpleName, "Error while updating profile", e)
            Toast.makeText(activity, "Error while updating profile", Toast.LENGTH_SHORT).show()
        }
    }

    fun getBoardDetails(activity: TaskListActivity, documentId: String) {
        mFireStore.collection(Constants.BOARDS).document(documentId).get().addOnSuccessListener {
            document ->
            Log.i(activity.javaClass.simpleName, document.toString())
            val board = document.toObject(Board::class.java)
            board!!.documentId = document.id
            activity.boardDetails(board)
        }.addOnFailureListener {
            e ->
            activity.hideProgressDialog()
            Log.e(activity.javaClass.simpleName, "Error getting Board Details", e)
        }
    }

    fun addUpdateTaskList(activity: Activity, board: Board) {
        val taskListHashMap = HashMap<String, Any>()
        taskListHashMap[Constants.TASK_LIST] = board.taskList

        mFireStore.collection(Constants.BOARDS).document(board.documentId).update(taskListHashMap).addOnSuccessListener {
            Log.i(activity.javaClass.simpleName, "TaskList updated successfully")
            if (activity is TaskListActivity) activity.addUpdateTaskListSuccess()
            else if (activity is CardDetailsActivity) activity.addUpdateTaskListSuccess()
        }. addOnFailureListener {
            exception ->
            if (activity is TaskListActivity) activity.hideProgressDialog()
            else if (activity is CardDetailsActivity) activity.hideProgressDialog()
            Log.e(activity.javaClass.simpleName, "Error while creating a board", exception)
        }
    }

    fun getAssignedMembersListDetails(activity: Activity, assignedTo: ArrayList<String>) {
        mFireStore.collection(Constants.USERS).whereIn(Constants.ID, assignedTo).get().addOnSuccessListener { document ->
            Log.i("Member List: ", document.documents.toString())
            val usersList: ArrayList<User> = ArrayList()

            for (i in document.documents) {
                val user = i.toObject(User::class.java)!!
                usersList.add(user)
            }

            if (activity is MembersActivity) activity.setUpMembersList(usersList)
            else if (activity is TaskListActivity) activity.boardMembersDetailsList(usersList)
        }.addOnFailureListener { e ->
            if (activity is MembersActivity) activity.hideProgressDialog()
            else if (activity is TaskListActivity) activity.hideProgressDialog()
            Log.e("Member List: ", "Error ", e)
        }
    }

    fun getMemberDetails(activity: MembersActivity, email: String) {
        mFireStore.collection(Constants.USERS).whereEqualTo(Constants.EMAIL, email).get().addOnSuccessListener {document ->
            if (document.documents.size > 0) {
                val user = document.documents[0].toObject(User::class.java)!!
                activity.memberDetails(user)
            } else {
                activity.hideProgressDialog()
                activity.showErrorSnackBar("Member does not exist")
            }
        }.addOnFailureListener { e ->
            Log.e("Fire Store Class: ", "Error while getting member details", e)
        }
    }

    fun assignMemberToBoard(activity: MembersActivity, board: Board, user: User) {
        val assignedToHashMap = HashMap<String, Any>()
        assignedToHashMap[Constants.ASSIGNED_TO] = board.assignedTo

        mFireStore.collection(Constants.BOARDS).document(board.documentId).update(assignedToHashMap).addOnSuccessListener { activity.memberAssignSuccess(user) }.addOnFailureListener {e ->
            Log.e("Fire Store Class: ", "Error assigning member to board", e)
        }
    }



}