package docSharing.repository;

import docSharing.entities.file.UpdateLog;
import docSharing.entities.permission.Authorization;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface UpdateLogRepository extends JpaRepository<UpdateLog, Integer> {

    @Query("SELECT u FROM UpdateLog u WHERE u.document.id=?1")
    List<Authorization> findByDocument(int documentId);

    @Transactional
    @Modifying
    @Query("DELETE UpdateLog u WHERE u.document.id=?1")
    void deleteByDocumentId(int documentId);
}