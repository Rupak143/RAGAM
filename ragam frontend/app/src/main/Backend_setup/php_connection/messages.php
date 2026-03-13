<?php
/**
 * Messages/Inbox API
 * Handles messaging between users
 */

include_once 'config.php';

header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: POST, GET, OPTIONS');
header('Access-Control-Allow-Headers: Content-Type');
header('Content-Type: application/json');

if ($_SERVER['REQUEST_METHOD'] == 'OPTIONS') {
    exit(0);
}

$database = new DatabaseConfig();
$db = $database->getConnection();

$method = $_SERVER['REQUEST_METHOD'];
$action = isset($_GET['action']) ? $_GET['action'] : '';

switch($method) {
    case 'GET':
        if($action == 'inbox') {
            getInboxMessages($db);
        } elseif($action == 'conversation') {
            getConversation($db);
        } else {
            APIResponse::error("Invalid action");
        }
        break;
    case 'POST':
        if($action == 'send') {
            sendMessage($db);
        } elseif($action == 'mark_read') {
            markAsRead($db);
        } else {
            APIResponse::error("Invalid action");
        }
        break;
    default:
        APIResponse::error("Method not allowed", 405);
        break;
}

function getInboxMessages($db) {
    $user_id = isset($_GET['user_id']) ? $_GET['user_id'] : '';
    
    if(empty($user_id)) {
        APIResponse::error("User ID is required");
        return;
    }
    
    try {
        $query = "SELECT m.message_id, m.sender_id, m.subject, m.message_text, m.is_read, m.sent_at,
                         u.full_name as sender_name, u.profile_image as sender_image,
                         c.course_title
                  FROM messages m
                  JOIN users u ON m.sender_id = u.user_id
                  LEFT JOIN courses c ON m.course_id = c.course_id
                  WHERE m.receiver_id = :user_id
                  ORDER BY m.sent_at DESC";
        
        $stmt = $db->prepare($query);
        $stmt->bindParam(":user_id", $user_id);
        $stmt->execute();
        
        $messages = $stmt->fetchAll(PDO::FETCH_ASSOC);
        
        APIResponse::success($messages, "Inbox messages retrieved successfully");
    } catch(PDOException $e) {
        APIResponse::error("Database error: " . $e->getMessage());
    }
}

function getConversation($db) {
    $user_id = isset($_GET['user_id']) ? $_GET['user_id'] : '';
    $other_user_id = isset($_GET['other_user_id']) ? $_GET['other_user_id'] : '';
    
    if(empty($user_id) || empty($other_user_id)) {
        APIResponse::error("Both user IDs are required");
        return;
    }
    
    try {
        $query = "SELECT m.message_id, m.sender_id, m.receiver_id, m.subject, m.message_text, 
                         m.is_read, m.sent_at,
                         u.full_name as sender_name, u.profile_image as sender_image
                  FROM messages m
                  JOIN users u ON m.sender_id = u.user_id
                  WHERE (m.sender_id = :user_id AND m.receiver_id = :other_user_id)
                     OR (m.sender_id = :other_user_id AND m.receiver_id = :user_id)
                  ORDER BY m.sent_at ASC";
        
        $stmt = $db->prepare($query);
        $stmt->bindParam(":user_id", $user_id);
        $stmt->bindParam(":other_user_id", $other_user_id);
        $stmt->execute();
        
        $messages = $stmt->fetchAll(PDO::FETCH_ASSOC);
        
        APIResponse::success($messages, "Conversation retrieved successfully");
    } catch(PDOException $e) {
        APIResponse::error("Database error: " . $e->getMessage());
    }
}

function sendMessage($db) {
    $data = json_decode(file_get_contents("php://input"));
    
    if(empty($data->sender_id) || empty($data->receiver_id) || empty($data->message_text)) {
        APIResponse::error("Sender ID, receiver ID and message text are required");
        return;
    }
    
    try {
        $query = "INSERT INTO messages (sender_id, receiver_id, course_id, subject, message_text) 
                  VALUES (:sender_id, :receiver_id, :course_id, :subject, :message_text)";
        
        $stmt = $db->prepare($query);
        $stmt->bindParam(":sender_id", $data->sender_id);
        $stmt->bindParam(":receiver_id", $data->receiver_id);
        $stmt->bindParam(":course_id", isset($data->course_id) ? $data->course_id : null);
        $stmt->bindParam(":subject", isset($data->subject) ? $data->subject : null);
        $stmt->bindParam(":message_text", $data->message_text);
        
        if($stmt->execute()) {
            APIResponse::success(['message_id' => $db->lastInsertId()], "Message sent successfully");
        } else {
            APIResponse::error("Failed to send message");
        }
    } catch(PDOException $e) {
        APIResponse::error("Database error: " . $e->getMessage());
    }
}

function markAsRead($db) {
    $data = json_decode(file_get_contents("php://input"));
    
    if(empty($data->message_id) || empty($data->user_id)) {
        APIResponse::error("Message ID and user ID are required");
        return;
    }
    
    try {
        $query = "UPDATE messages SET is_read = 1 
                  WHERE message_id = :message_id AND receiver_id = :user_id";
        
        $stmt = $db->prepare($query);
        $stmt->bindParam(":message_id", $data->message_id);
        $stmt->bindParam(":user_id", $data->user_id);
        
        if($stmt->execute()) {
            APIResponse::success(null, "Message marked as read");
        } else {
            APIResponse::error("Failed to mark message as read");
        }
    } catch(PDOException $e) {
        APIResponse::error("Database error: " . $e->getMessage());
    }
}
?>
