package com.example.library.controller.mainScreen;

import com.example.library.controller.login.Login_LoginController;
import com.example.library.model.Account;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import com.example.library.model.DatabaseHelper;
import com.example.library.model.Document;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Optional;
import javafx.scene.control.TextInputDialog;

import java.awt.Desktop;

import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;

import static com.example.library.model.Alert.showInfoAlert;
import static com.example.library.model.Alert.showWarningAlert;
import static com.example.library.model.SoundUtil.applySoundEffectsToButtons;

public class BorrowController extends Controller {
    @FXML
    private HBox root;
    @FXML
    private TextField searchExploreField;
    @FXML
    private ListView<String> suggestionsList;
    @FXML
    private ImageView myImageView;
    @FXML
    private TextField documentIdField;
    @FXML
    private TextField titleField;
    @FXML
    private TextField authorField;
    @FXML
    private TextField publicYearField;
    @FXML
    private TextField publisherField;
    @FXML
    private TextField genreField;
    @FXML
    private TextField quantityField;
    @FXML
    private Hyperlink myHyperlink;
    @FXML
    private Button showButton;
    @FXML
    private Button homeButton;
    @FXML
    private Button exploreButton;
    @FXML
    private Button gameButton;
    @FXML
    private Button borrowButton;
    @FXML
    private Button returnButton;


    private DatabaseHelper databaseHelper;

    public BorrowController() {
        // Khởi tạo databaseHelper
        this.databaseHelper = new DatabaseHelper(); // Đảm bảo khởi tạo đúng cách
    }


    private boolean isListViewVisible = false; // Track visibility state
    private String googleSearchUrl;

    @Override
    public void initialize() {
        databaseHelper = new DatabaseHelper();

        // Lắng nghe sự kiện nhập liệu trong TextField
        suggestionsList.setVisible(false); // Ẩn ListView
        searchExploreField.textProperty().addListener((observable, oldValue, newValue) -> {
            showButton.setText("Hide");
            if (newValue != null && !newValue.isEmpty()) {
                updateSuggestions(newValue);
            } else {
                suggestionsList.getItems().clear(); // Xóa gợi ý khi không có nội dung
                suggestionsList.setVisible(false); // Ẩn ListView
            }
        });

        // Lắng nghe sự kiện chọn mục trong ListView
        suggestionsList.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                loadDocumentDetails(newValue); // Gọi phương thức để nạp thông tin tài liệu
                searchExploreField.setText(newValue);
                suggestionsList.setVisible(false); // Ẩn ListView sau khi chọn
                showButton.setText("Show");
            } else {
                System.out.println("Không có mục nào được chọn.");
            }
        });

        searchExploreField.addEventFilter(MouseEvent.MOUSE_CLICKED, event -> {
            String currentText = searchExploreField.getText();
            // Kiểm tra nếu không phải là dấu cách ở cuối
            if (!currentText.endsWith(" ")) {
                searchExploreField.setText(currentText + " "); // Thêm dấu cách
                // Đặt con trỏ ở cuối văn bản
                searchExploreField.positionCaret(currentText.length() + 1); // +1 để đặt con trỏ sau dấu cách
            }
            showButton.setText("Show");
        });

        myHyperlink.setOnAction(event -> handleHyperlinkAction());
        showButton.setOnAction(actionEvent -> handleShowButton());
        homeButton.setOnAction(actionEvent -> handleHomeButton());
        exploreButton.setOnAction(actionEvent -> handleExploreButton());
        gameButton.setOnAction(actionEvent -> handleGameButton());
        borrowButton.setOnAction(actionEvent -> handleBorrowButton());
        returnButton.setOnAction(actionEvent -> handleReturnButton());

        if (Controller.isDarkMode()) {
            root.getStylesheets().clear();
            root.getStylesheets().add(Objects.requireNonNull(
                    getClass().getResource("/CSSStyling/dark_explore.css")).toExternalForm());
        } else {
            root.getStylesheets().clear();
            root.getStylesheets().add(Objects.requireNonNull(
                    getClass().getResource("/CSSStyling/explore.css")).toExternalForm());
        }

        applySoundEffectsToButtons(root);
    }

    private void handleShowButton() {
        isListViewVisible = !isListViewVisible; // Toggle visibility state
        suggestionsList.setVisible(isListViewVisible);
        suggestionsList.setManaged(isListViewVisible);
        showButton.setText(isListViewVisible ? "Hide" : "Show"); // Update button text
    }

    // Phương thức cập nhật gợi ý
    private void updateSuggestions(String query) {
        // Create a Task to run the database query in a background thread
        Task<List<String>> task = new Task<List<String>>() {
            @Override
            protected List<String> call() throws Exception {
                // Fetch suggestions from the database
                return databaseHelper.getSuggestions(query);
            }

            @Override
            protected void succeeded() {
                super.succeeded();
                try {
                    // Get the result from the task
                    List<String> suggestions = get();
                    ObservableList<String> observableSuggestions = FXCollections.observableArrayList(suggestions);

                    // Update ListView with new suggestions
                    suggestionsList.setItems(observableSuggestions);
                    suggestionsList.setVisible(!observableSuggestions.isEmpty());
                    suggestionsList.setManaged(!observableSuggestions.isEmpty());

                    System.out.println("Số lượng gợi ý: " + observableSuggestions.size());
                } catch (InterruptedException | ExecutionException e) {
                    e.printStackTrace(); // Handle exceptions
                }
            }

            @Override
            protected void failed() {
                super.failed();
                // Handle failure case
                System.err.println("Error fetching suggestions.");
            }
        };

        // Run the task in a new thread
        new Thread(task).start();
    }


    public void loadDocumentDetails(String title) {
        Document document = databaseHelper.getDocumentByTitle(title); // Lấy tài liệu theo tiêu đề
        if (document != null) {
            // Cập nhật các trường với thông tin tài liệu
            documentIdField.setText(String.valueOf(document.getId()));
            titleField.setText(document.getTitle());
            authorField.setText(document.getAuthor());
            publicYearField.setText(String.valueOf(document.getPublicYear()));
            publisherField.setText(document.getPublisher());
            genreField.setText(document.getGenre());
            quantityField.setText(String.valueOf(document.getQuantity()));

            // Cập nhật hình ảnh document.getImageLink()
            String imageLink = document.getImageLink();
            if (imageLink == null || imageLink.isEmpty()) {
                showWarningAlert("Không có hình ảnh");
            } else {
                Image image = new Image(imageLink, true);
                myImageView.setImage(image);
                // myImageView.setSmooth(true); // Bạn có thể bật làm mịn nếu cần
            }

            googleSearchUrl = "https://www.google.com/search?q=" + (title + " book").replace(" ", "+");
        } else {
            // Hiển thị thông báo lỗi cho người dùng
            System.out.println("Không tìm thấy tài liệu. Vui lòng thử với tiêu đề khác.");
        }
    }

    @FXML
    private void handleHyperlinkAction() {
        searchInGoogleByLink(googleSearchUrl);
    }

    private void searchInGoogleByLink(String googleSearchUrl) {
        try {
            if (googleSearchUrl != null) {
                Desktop.getDesktop().browse(new URI(googleSearchUrl));
                System.out.println(googleSearchUrl);
            } else {
                showWarningAlert("Vui lòng chọn nội dung tìm kiếm");
            }
        } catch (IOException | URISyntaxException e) {
            e.printStackTrace();
        }
    }

    private void handleBorrowButton() {
        String username = Login_LoginController.usernameToBorrow;
        Account account = databaseHelper.getAccountByUserName(username);
        int userId;
        if (account == null) {
            userId = 1; // Default user ID
        } else {
            System.out.println("have acc");
            userId = account.getId();
        }
        int documentId = Integer.parseInt(documentIdField.getText());
        LocalDate borrowDate = LocalDate.now();
        LocalDate requiredDate = borrowDate.plusMonths(1); // Calculate requiredDate as borrowDate + 1 month

        // Display dialog to input quantity
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Enter the amount borrowed");
        dialog.setHeaderText("Enter the amount you want to borrow:");
        dialog.setContentText("Quantity:");

        Optional<String> result = dialog.showAndWait();
        if (result.isPresent()) {
            try {
                int quantityToBorrow = Integer.parseInt(result.get());

                Document document = databaseHelper.getDocumentById(documentId);
                if (document != null) {
                    int currentQuantity = document.getQuantity();
                    if (currentQuantity >= quantityToBorrow) {
                        // Check if the record already exists
                        String sqlCheckExists = "SELECT quantityBorrow FROM Borrow_Return WHERE user_id = ? AND document_id = ?";

                        try (Connection conn = databaseHelper.connect();
                             PreparedStatement pstmtCheck = conn.prepareStatement(sqlCheckExists)) {

                            pstmtCheck.setInt(1, userId);
                            pstmtCheck.setInt(2, documentId);
                            ResultSet rs = pstmtCheck.executeQuery();

                            if (rs.next()) {
                                // If a record exists, inform the user
                                showWarningAlert("Previously borrowed books, please return in advance.");
                            } else {
                                // If no record exists, proceed to borrow
                                String sqlInsertBorrow = "INSERT INTO Borrow_Return (user_id, document_id, borrow_date, quantityBorrow, required_date, imageLink, title) VALUES (?,?,?,?,?,?,?)";
                                String sqlUpdateDocumentQuantity = "UPDATE Documents SET quantity = quantity - ? WHERE id = ?";

                                try (PreparedStatement pstmtInsert = conn.prepareStatement(sqlInsertBorrow);
                                     PreparedStatement pstmtUpdateDocument = conn.prepareStatement(sqlUpdateDocumentQuantity)) {

                                    // Add the borrowing information
                                    pstmtInsert.setInt(1, userId);
                                    pstmtInsert.setInt(2, documentId);
                                    pstmtInsert.setDate(3, Date.valueOf(borrowDate));
                                    pstmtInsert.setInt(4, quantityToBorrow); // Record the borrowed quantity
                                    pstmtInsert.setDate(5, Date.valueOf(requiredDate)); // Set required date
                                    pstmtInsert.setString(6, document.getImageLink());
                                    pstmtInsert.setString(7, document.getTitle());

                                    pstmtInsert.executeUpdate();

                                    // Update the document quantity
                                    pstmtUpdateDocument.setInt(1, quantityToBorrow);
                                    pstmtUpdateDocument.setInt(2, documentId);
                                    pstmtUpdateDocument.executeUpdate();

                                    // Cập nhật quantityField
                                    int updatedQuantity = document.getQuantity() - quantityToBorrow; // Assuming document has a getQuantity() method
                                    quantityField.setText(String.valueOf(updatedQuantity));

                                    showInfoAlert("NOTIFICATION", "Congratulation", "Borrowed book successfully");
                                } catch (SQLException e) {
                                    e.printStackTrace();
                                }
                            }
                        } catch (SQLException e) {
                            e.printStackTrace();
                        }
                    } else {
                        showWarningAlert("The borrowed quantity exceeds the available quantity.");
                    }
                } else {
                    System.out.println("Document not found.");
                }
            } catch (NumberFormatException e) {
                System.out.println("Please enter a valid quantity.");
            }
        }
    }

    private void handleReturnButton() {
        String username = Login_LoginController.usernameToBorrow;
        Account account = databaseHelper.getAccountByUserName(username);

        int userId;
        if (account == null) {
            userId = 1;
        } else {
            System.out.println("have acc");
            userId = account.getId();
        }
        int documentId = Integer.parseInt(documentIdField.getText());

        // Hiển thị hộp thoại yêu cầu nhập số lượng
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Enter the amount to return");
        dialog.setHeaderText("Enter the amount you want to pay:");
        dialog.setContentText("Quantity:");

        Optional<String> result = dialog.showAndWait();
        if (result.isPresent()) {
            try {
                int quantityToReturn = Integer.parseInt(result.get());

                // Lấy thông tin tài liệu để kiểm tra số lượng đã mượn
                Document document = databaseHelper.getDocumentById(documentId);
                if (document != null) {
                    int borrowedQuantity = databaseHelper.getBorrowedQuantity(userId, documentId);

                    if (borrowedQuantity >= quantityToReturn) {
                        String sqlUpdateReturnDate = "UPDATE borrow_return SET quantityBorrow = quantityBorrow - ? " +
                                "WHERE user_id = ? AND document_id = ?";
                        String sqlDeleteReturn = "DELETE FROM borrow_return WHERE user_id = ? AND document_id = ?";
                        String sqlUpdateQuantity = "UPDATE Documents SET quantity = quantity + ? WHERE id = ?";

                        try (Connection conn = databaseHelper.connect();
                             PreparedStatement pstmtUpdateDate = conn.prepareStatement(sqlUpdateReturnDate);
                             PreparedStatement pstmtDelete = conn.prepareStatement(sqlDeleteReturn);
                             PreparedStatement pstmtUpdate = conn.prepareStatement(sqlUpdateQuantity)) {

                            // Cập nhật số lượng đã mượn
                            pstmtUpdateDate.setInt(1, quantityToReturn);
                            pstmtUpdateDate.setInt(2, userId);
                            pstmtUpdateDate.setInt(3, documentId);
                            pstmtUpdateDate.executeUpdate();

                            // Cập nhật số lượng tài liệu
                            pstmtUpdate.setInt(1, quantityToReturn);
                            pstmtUpdate.setInt(2, documentId);
                            pstmtUpdate.executeUpdate();

                            // Kiểm tra và xóa nếu quantityBorrow <= 0
                            int newBorrowedQuantity = borrowedQuantity - quantityToReturn;
                            if (newBorrowedQuantity <= 0) {
                                pstmtDelete.setInt(1, userId);
                                pstmtDelete.setInt(2, documentId);
                                pstmtDelete.executeUpdate();
                            }

                            // Cập nhật quantityField
                            int updatedQuantity = document.getQuantity() + quantityToReturn; // Assuming document has a getQuantity() method
                            quantityField.setText(String.valueOf(updatedQuantity));

                            showInfoAlert("NOTIFICATION", "Congratulations", "Returned book successfully");
                        } catch (SQLException e) {
                            e.printStackTrace();
                            showWarningAlert("An error occurred while returning the book.");
                        }
                    } else {
                        showWarningAlert("The amount returned exceeds the amount borrowed.");
                    }
                } else {
                    showWarningAlert("No documents found.");
                }
            } catch (NumberFormatException e) {
                showWarningAlert("Please enter a valid number.");
            }
        }
    }

    @Override
    protected void changeScene(String fxmlPath, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();
            Stage stage = (Stage) exploreButton.getScene().getWindow();
            Scene scene = new Scene(root, 1300, 650);
            stage.setTitle(title);
            stage.setScene(scene);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}