package com.storyworld.domain.elastic;

import javax.persistence.Id;
import javax.validation.constraints.NotNull;

import org.hibernate.validator.constraints.Length;
import org.springframework.data.elasticsearch.annotations.Document;

import com.storyworld.domain.sql.User;

import lombok.Data;

@Data
@Document(indexName = "comment", type = "comment")
public class CommentContent {

	@Id
	private String id;

	@NotNull
	@Length(min = 4, max = 255)
	private String content;

	private User author;

	private int likes;

	private int dislikes;

	private boolean edited;

	private String date;

	private Long storyId;

}
