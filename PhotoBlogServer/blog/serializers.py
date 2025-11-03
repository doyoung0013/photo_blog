from rest_framework import serializers
from .models import Post, Comment

class CommentSerializer(serializers.ModelSerializer):
    class Meta:
        model = Comment
        fields = ['id', 'nickname', 'content', 'created_at']


class PostSerializer(serializers.ModelSerializer):
    image = serializers.SerializerMethodField()
    comments = CommentSerializer(many=True, read_only=True)

    def get_image(self, obj):
        request = self.context.get('request')
        if obj.image:
            return request.build_absolute_uri(obj.image.url)
        return None

    class Meta:
        model = Post
        fields = ['id', 'title', 'text', 'image', 'published_date', 'like_count', 'comments']
